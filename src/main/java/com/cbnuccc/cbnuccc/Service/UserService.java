package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.cbnuccc.cbnuccc.Config.MailgunProperties;
import com.cbnuccc.cbnuccc.Config.SecurityConfig;
import com.cbnuccc.cbnuccc.Config.SupabaseProperties;
import com.cbnuccc.cbnuccc.Dto.LimitedUserDto;
import com.cbnuccc.cbnuccc.Dto.OldAndNewPasswordDto;
import com.cbnuccc.cbnuccc.Dto.ResetPasswordDto;
import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Dto.UserDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.ReviewSoonInfo;
import com.cbnuccc.cbnuccc.Model.Verification;
import com.cbnuccc.cbnuccc.Repository.MissionJpaRepository;
import com.cbnuccc.cbnuccc.Repository.PrayerJpaRepository;
import com.cbnuccc.cbnuccc.Repository.ReviewSoonInfoJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Repository.VerificationJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.SecurityUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserJpaRepository userJpaRepository;
    private final VerificationJpaRepository verificationJpaRepository;
    private final PrayerJpaRepository prayerJpaRepository;
    private final MissionJpaRepository missionJpaRepository;
    private final ReviewSoonInfoJpaRepository reviewSoonInfoJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final WebClient webClient;
    private final SupabaseProperties supabaseProperties;
    private final SecurityConfig securityConfig;
    private final MailgunProperties mailgunProperties;

    // User를 UserDto로 변환하기
    // 세션이 열려 있는 동안(트랜잭션 내부) 호출해야 함 - lazy 로딩되는 affiliatedReviewSoon을
    // 프록시 상태 그대로 DTO에 담으면 세션 종료 후 직렬화 시점에 LazyInitializationException 발생
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null) - 대표 여부 계산에 사용됨
    private UserDto userToUserDto(MyUser user, UUID callerUuid) {
        return new UserDto(
                user.getUuid(),
                user.getEmail(),
                user.getRank(),
                user.getSex(),
                user.getName(),
                user.getGrade(),
                prayerJpaRepository.countByAuthorUuid(user.getUuid()),
                missionJpaRepository.countByAuthorUuid(user.getUuid()),
                reviewSoonInfoToDto(user.getAffiliatedReviewSoon(), callerUuid));
    }

    // ReviewSoonInfo를 ReviewSoonInfoDto로 변환하기
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null) - 대표 여부 계산에 사용됨
    private ReviewSoonInfoDto reviewSoonInfoToDto(ReviewSoonInfo reviewSoonInfo, UUID callerUuid) {
        if (reviewSoonInfo == null)
            return null;

        MyUser representative = reviewSoonInfo.getRepresentative();
        boolean isRepresentative = callerUuid != null
                && representative != null
                && callerUuid.equals(representative.getUuid());

        return new ReviewSoonInfoDto(reviewSoonInfo.getId(), reviewSoonInfo.getName(), isRepresentative);
    }

    // UserDto를 User로 변환하기
    private MyUser userDtoToUser(UserDto userDto) {
        MyUser user = new MyUser();
        user.setEmail(userDto.getEmail());
        user.setUuid(userDto.getUuid());
        user.setRank(userDto.getRank());
        user.setSex(userDto.getSex());
        user.setName(userDto.getName());
        user.setGrade(userDto.getGrade());
        return user;
    }

    // UserDto를 LimitedUserDto로 변환하기
    private LimitedUserDto userDtoToLimitedUserDto(UserDto userDto) {
        LimitedUserDto dto = new LimitedUserDto(
                userDto.getUuid(),
                userDto.getRank(),
                userDto.getName(),
                userDto.getGrade(),
                userDto.getPrayerCount(),
                userDto.getMissionCount(),
                userDto.getAffiliatedReviewSoon());
        return dto;
    }

    // LimitedUserDto를 UserDto로 변환하기
    private UserDto limitedUserDtoToUserDto(LimitedUserDto limitedUserDto) {
        UserDto dto = new UserDto();
        dto.setUuid(limitedUserDto.getUuid());
        dto.setRank(limitedUserDto.getRank());
        dto.setName(limitedUserDto.getName());
        dto.setGrade(limitedUserDto.getGrade());
        return dto;
    }

    // 주어진 사용자에 담긴 소속 점검순 id로 실제 존재하는 점검순을 찾아 반환하기
    // 소속 점검순이 주어지지 않았다면(null) DataWithStatusCode의 data가 null인 상태로 성공 반환
    private DataWithStatusCode<ReviewSoonInfo> resolveAffiliatedReviewSoon(MyUser user) {
        ReviewSoonInfo givenReviewSoon = user.getAffiliatedReviewSoon();
        if (givenReviewSoon == null || givenReviewSoon.getId() == null)
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, null);

        Optional<ReviewSoonInfo> _reviewSoonInfo = reviewSoonInfoJpaRepository.findById(givenReviewSoon.getId());
        if (_reviewSoonInfo.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_REVIEW_SOON_FOUND, null);

        return new DataWithStatusCode<>(StatusCode.NO_ERROR, _reviewSoonInfo.get());
    }

    // 사용자의 비밀번호를 암호화하기
    private MyUser encodeUserPassword(MyUser user, String planePassword) {
        String encodedPassword = passwordEncoder.encode(securityUtil.addPepper(planePassword));
        user.setPassword(encodedPassword);
        return user;
    }

    // 사용자의 학번을 암호화하기
    private MyUser encodeUserStudentId(MyUser user, String planeStudentId) {
        String encodedStudentId = passwordEncoder.encode(securityUtil.addPepper(planeStudentId));
        user.setStudentId(encodedStudentId);
        return user;
    }

    // 이메일로 사용자의 중복 여부 확인하기
    private boolean checkDuplicatedUserByEmail(String email) {
        Optional<MyUser> user = userJpaRepository.findByEmail(email.toLowerCase());
        return user.isPresent();
    }

    // 이메일이 인증되었다면 true를 반환
    // 그렇지 않다면 false를 반환
    // 또한, 이메일이 DB에 없다면 false를 반환
    private boolean checkIsVerifiedEmail(String email) {
        Optional<Verification> _verification = verificationJpaRepository.findByEmail(email.toLowerCase());
        if (_verification.isEmpty())
            return false;
        Verification verification = _verification.get();
        return verification.getIsVerified();
    }

    // 주어진 uuid로 UserDto 찾기
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null)
    @Transactional
    public Optional<UserDto> findUserDtoByUuid(UUID uuid, UUID callerUuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        UserDto result = userToUserDto(_user.get(), callerUuid);
        return Optional.of(result);
    }

    // 주어진 이메일로 UserDto 찾기
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null)
    @Transactional
    public Optional<UserDto> findUserDtoByEmail(String email, UUID callerUuid) {
        Optional<MyUser> _user = userJpaRepository.findByEmail(email.toLowerCase());
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        UserDto result = userToUserDto(_user.get(), callerUuid);
        return Optional.of(result);
    }

    // 주어진 uuid로 LimitedUserDto 찾기
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null)
    @Transactional
    public Optional<LimitedUserDto> findLimitedUserDtoByUuid(UUID uuid, UUID callerUuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return Optional.ofNullable(null);
        LimitedUserDto result = userDtoToLimitedUserDto(userToUserDto(_user.get(), callerUuid));
        return Optional.of(result);
    }

    // 주어진 UserDto와 일치하는 모든 사용자 찾기
    // callerUuid: 요청을 보낸 본인의 uuid (로그인하지 않은 요청이라면 null)
    @Transactional
    public Page<LimitedUserDto> findAllLimitedUserDtosByLimitedUserDto(LimitedUserDto exampleUser, Pageable pageable,
            UUID callerUuid) {
        // LimitedUserDto를 User로 변환하기
        MyUser example = userDtoToUser(limitedUserDtoToUserDto(exampleUser));
        Page<MyUser> users = userJpaRepository.findAll(Example.of(example), pageable);

        return users.map(user -> userDtoToLimitedUserDto(userToUserDto(user, callerUuid)));
    }

    // 주어진 jwt 토큰에서 uuid 가져오기
    public UUID getUuidFromAuth(Authentication authentication) {
        String uuidString = (String) authentication.getPrincipal();
        UUID uuid = UUID.fromString(uuidString);
        return uuid;
    }

    // 사용자 생성하기
    @Transactional
    public DataWithStatusCode<LimitedUserDto> createUser(MyUser user) {
        user.setUuid(UUID.randomUUID());
        String email = user.getEmail().toLowerCase();

        if (checkDuplicatedUserByEmail(email))
            return new DataWithStatusCode<>(StatusCode.DUPLICATED_EMAIL, null);

        if (!checkIsVerifiedEmail(email))
            return new DataWithStatusCode<>(StatusCode.NOT_VERIFIED, null);

        // 비밀번호와 학번을 확인하고 암호화하기
        if (!securityUtil.checkValidPassword(user.getPassword()))
            return new DataWithStatusCode<>(StatusCode.INVALID_PASSWORD, null);

        // 소속 점검순이 주어졌다면, 실제 존재하는 점검순인지 확인 후 연결하기
        DataWithStatusCode<ReviewSoonInfo> resolvedReviewSoon = resolveAffiliatedReviewSoon(user);
        if (resolvedReviewSoon.code().checkIsError())
            return new DataWithStatusCode<>(resolvedReviewSoon.code(), null);
        user.setAffiliatedReviewSoon(resolvedReviewSoon.data());

        user = encodeUserPassword(user, user.getPassword());
        user = encodeUserStudentId(user, user.getStudentId());

        user.setPasswordChangedAt(OffsetDateTimeUtil.getNow());

        try {
            MyUser createdUser = userJpaRepository.save(user);
            verificationJpaRepository.deleteByEmail(email); // 인증 테이블에서 인증된 사용자 삭제하기
            // 회원가입은 로그인 전 이루어지므로 본인(caller) 개념이 없음 -> isRepresentative는 항상 false
            LimitedUserDto createdLimitedUserDto = userDtoToLimitedUserDto(userToUserDto(createdUser, null));
            return new DataWithStatusCode<LimitedUserDto>(StatusCode.NO_ERROR, createdLimitedUserDto);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_USER, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // uuid로 사용자를 주어진 사용자 정보로 수정하기
    // 주어진 사용자의 필드 값이 null이라면,
    // 사용자의 해당 필드는 변경되지 않음
    @Transactional
    public StatusCode updateUserByUuid(UUID uuid, MyUser user) {
        Optional<MyUser> _oldUser = userJpaRepository.findByUuid(uuid);
        if (_oldUser.isEmpty())
            return StatusCode.NO_USER_FOUND;

        MyUser oldUser = _oldUser.get();
        if (user.getId() != null ||
                user.getUuid() != null ||
                user.getStudentId() != null ||
                user.getPassword() != null)
            return StatusCode.CONNOT_CHANGE_IMPORTANT_INFORMATION;

        // 필드 값이 null이 아니라면 변경하기
        if (user.getEmail() != null)
            oldUser.setEmail(user.getEmail().toLowerCase());
        if (user.getRank() != null)
            oldUser.setRank(user.getRank());
        if (user.getSex() != null)
            oldUser.setSex(user.getSex());
        if (user.getName() != null)
            oldUser.setName(user.getName());
        if (user.getGrade() != null)
            oldUser.setGrade(user.getGrade());

        // 소속 점검순이 주어졌다면, 실제 존재하는 점검순인지 확인 후 변경하기
        if (user.getAffiliatedReviewSoon() != null) {
            DataWithStatusCode<ReviewSoonInfo> resolvedReviewSoon = resolveAffiliatedReviewSoon(user);
            if (resolvedReviewSoon.code().checkIsError())
                return resolvedReviewSoon.code();
            oldUser.setAffiliatedReviewSoon(resolvedReviewSoon.data());
        }

        userJpaRepository.save(oldUser);
        return StatusCode.NO_ERROR;
    }

    // uuid로 사용자의 비밀번호 수정하기
    @Transactional
    public StatusCode updateUserPasswordByUuid(UUID uuid, OldAndNewPasswordDto passwords) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND;
        MyUser user = _user.get();

        // 마지막으로 변경한 시점으로부터 5분이 지나야만 비밀번호를 변경할 수 있음
        if (user.getPasswordChangedAt().isAfter(OffsetDateTimeUtil.getNow().minusMinutes(5)))
            return StatusCode.CANNOT_CHANGE_PASSWORD_WITHIN_5_MINUTES;

        // 기존 비밀번호가 일치하는지 확인하기
        String oldPassword = securityUtil.addPepper(passwords.getOldPassword());
        boolean isMatchedPassword = securityConfig.passwordEncoder().matches(oldPassword, user.getPassword());
        if (!isMatchedPassword)
            return StatusCode.PASSWORD_IS_INCURRECT;

        // 주어진 새 비밀번호가 유효한지 확인하기
        boolean isValidPassword = securityUtil.checkValidPassword(passwords.getNewPassword());
        if (!isValidPassword)
            return StatusCode.INVALID_PASSWORD;

        // 비밀번호 변경하기
        user.setPasswordChangedAt(OffsetDateTimeUtil.getNow());
        user = encodeUserPassword(user, passwords.getNewPassword());
        userJpaRepository.save(user);
        return StatusCode.NO_ERROR;
    }

    // 비밀번호 초기화 이메일 전송하기
    @Transactional
    public StatusCode resetPassword(ResetPasswordDto resetPasswordDto) {
        // 일치하는 사용자 찾기
        Optional<MyUser> _user = userJpaRepository.findByEmail(resetPasswordDto.getEmail().toLowerCase());
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND;
        MyUser user = _user.get();

        // 올바른 사용자인지 확인하기
        if (!resetPasswordDto.getName().equals(user.getName()))
            return StatusCode.NO_USER_FOUND;

        if (!passwordEncoder.matches(securityUtil.addPepper(resetPasswordDto.getStudentId()), user.getStudentId()))
            return StatusCode.NO_USER_FOUND;

        // 마지막으로 변경한 시점으로부터 5분이 지나야만 비밀번호를 변경할 수 있음
        if (user.getPasswordChangedAt().isAfter(OffsetDateTimeUtil.getNow().minusMinutes(5)))
            return StatusCode.CANNOT_CHANGE_PASSWORD_WITHIN_5_MINUTES;

        // 새 비밀번호 생성하기
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specials = "!@#$%^&*()-_=+[]{};:,.<>?";

        StringBuilder password = new StringBuilder();

        // 무작위 알파벳(대/소문자) 4개 추가하기
        for (int i = 0; i < 4; i++) {
            char c = (Math.random() < 0.5 ? upper : lower).charAt((int) (Math.random() * 26));
            password.append(c);
        }

        // 무작위 숫자 5개 추가하기
        for (int i = 0; i < 5; i++) {
            password.append(digits.charAt((int) (Math.random() * digits.length())));
        }

        // 무작위 특수문자 2개 추가하기
        for (int i = 0; i < 2; i++) {
            password.append(specials.charAt((int) (Math.random() * specials.length())));
        }

        // 비밀번호 섞기
        char[] passwordChars = password.toString().toCharArray();
        for (int i = passwordChars.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = passwordChars[i];
            passwordChars[i] = passwordChars[j];
            passwordChars[j] = temp;
        }
        String newPassword = new String(passwordChars);

        // 비밀번호 암호화 및 수정하기
        user = encodeUserPassword(user, newPassword);
        userJpaRepository.save(user);

        // 결과를 알리는 이메일 전송하기
        String messageHeader = "안녕하세요!\n충북대학교 CCC입니다.\n아래와 같이 비밀번호가 초기화되었음을 알려드립니다.";
        String messageCode = "새 비밀번호: " + newPassword;
        String messageFooter = "위 비밀번호를 아무에게도 공개하지 마세요!\n로그인하신 후 즉시 비밀번호를 변경해주세요.\n감사합니다.";
        final String apiKey = mailgunProperties.getKey();
        final String senderDomain = mailgunProperties.getDomain();
        try {
            HttpResponse<JsonNode> request = Unirest
                    .post("https://api.mailgun.net/v3/" + senderDomain + "/messages")
                    .basicAuth("api", apiKey)
                    .queryString("from", "CBNU CCC <postmaster@" + senderDomain + ">")
                    .queryString("to", user.getEmail())
                    .queryString("subject", "[CBNU CCC] 🌱 비밀번호가 초기화되었습니다.")
                    .queryString("text",
                            messageHeader + "\n\n" + messageCode + "\n\n" + messageFooter)
                    .asJson();

            // 상태 코드가 200이라는 것은 정상적으로 처리됨
            if (request.getStatus() != 200)
                return StatusCode.SOMETHING_WENT_WRONG;
        } catch (UnirestException e) {
            LogUtil.printBasicWarnLog(LogHeader.SEND_REGISTRATION_EMAIL, e.getMessage(), null);
            return StatusCode.SOMETHING_WENT_WRONG;
        }

        return StatusCode.NO_ERROR;
    }

    // uuid로 사용자 삭제하기
    @Transactional
    public StatusCode deleteUserByUuid(UUID uuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return StatusCode.NO_USER_FOUND;

        MyUser user = _user.get();
        try {
            deleteProfileImage(uuid); // 사용자 삭제에 따라 사용자의 프로필 사진 삭제하기
            userJpaRepository.delete(user);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_USER, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // uuid로 사용자 프로필 사진 업로드하기
    public StatusCode uploadProfileImage(MultipartFile file, UUID uuid) {
        try {
            // 확장자 추출하기
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains("."))
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 파일 이름 설정하기
            String fileName = uuid.toString();
            String path = "profile/" + fileName;

            // 업로드하기
            webClient.post()
                    .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                    .header("Authorization", "Bearer " + supabaseProperties.getKey())
                    .header("apikey", supabaseProperties.getKey())
                    .header("Content-Type", "image/" + extension)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // uuid로 사용자 프로필 사진 삭제하기
    public StatusCode deleteProfileImage(UUID uuid) {
        try {
            // 파일 이름 설정하기
            String fileName = uuid.toString();
            String path = "profile/" + fileName;

            // 삭제하기
            webClient.delete()
                    .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                    .header("Authorization", "Bearer " + supabaseProperties.getKey())
                    .header("apikey", supabaseProperties.getKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }
}
