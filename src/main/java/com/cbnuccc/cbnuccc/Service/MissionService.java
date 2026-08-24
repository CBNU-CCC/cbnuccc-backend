package com.cbnuccc.cbnuccc.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.cbnuccc.cbnuccc.Config.SupabaseProperties;
import com.cbnuccc.cbnuccc.Dto.MissionDto;
import com.cbnuccc.cbnuccc.Model.Mission;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Repository.MissionJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionService {
    private final MissionJpaRepository missionJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final WebClient webClient;
    private final SupabaseProperties supabaseProperties;

    private MissionDto missionToMissionDto(Mission mission) {
        return new MissionDto(
                mission.getId(),
                missionJpaRepository.findAuthorUuidByMissionId(mission.getId()).get(),
                mission.getCreatedAt(),
                mission.getSite(),
                mission.getStartTerm(),
                mission.getEndTerm(),
                mission.getSeason(),
                mission.getTestimony(),
                mission.getImageCount());
    }

    private void deleteSpecificMissionImage(long id, short imageId) {
        // 파일 이름 설정하기
        String fileName = String.format("%d-%d", id, imageId);
        String path = "mission/" + fileName;

        // 삭제하기
        webClient.delete()
                .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                .header("Authorization", "Bearer " + supabaseProperties.getKey())
                .header("apikey", supabaseProperties.getKey())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // 모든 선교 가져오기
    public Page<MissionDto> getAllMissions(Pageable pageable) {
        Page<Mission> missions = missionJpaRepository.findAll(pageable);
        return missions.map(mission -> missionToMissionDto(mission));
    }

    // 특정 선교 가져오기
    public DataWithStatusCode<MissionDto> getSpecificMission(long id) {
        Optional<Mission> _mission = missionJpaRepository.findById(id);
        if (_mission.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_MISSION_FOUND, null);
        Mission mission = _mission.get();
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, missionToMissionDto(mission));
    }

    // 내 모든 선교 가져오기
    public Page<MissionDto> getAllMyMissions(UUID uuid, Pageable pageable) {
        Page<Mission> missions = missionJpaRepository.findAllByAuthorUuid(uuid, pageable);
        return missions.map(mission -> missionToMissionDto(mission));
    }

    // 선교 생성하기
    public DataWithStatusCode<MissionDto> createMission(MissionDto missionDto, UUID uuid) {
        // 작성자 정보 찾기
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_USER_FOUND, null);
        MyUser user = _user.get();

        // 선교 인스턴스 생성하기
        Mission mission = new Mission();
        mission.setAuthor(user);
        mission.setCreatedAt(OffsetDateTimeUtil.getNow());
        mission.setSite(missionDto.getSite());
        mission.setStartTerm(missionDto.getStartTerm());
        mission.setEndTerm(missionDto.getEndTerm());
        mission.setSeason(missionDto.getSeason());
        mission.setTestimony(missionDto.getTestimony());
        mission.setImageCount((short) 0);

        try {
            // 저장하기
            Mission createdMission = missionJpaRepository.save(mission);
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, missionToMissionDto(createdMission));
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_MISSION, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // 선교 수정하기
    public StatusCode updateMission(long id, UUID uuid, MissionDto missionDto) {
        // 존재 여부 확인하기
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // 선교 수정하기
        missionDto.setCreatedAt(OffsetDateTimeUtil.getNow());
        if (missionDto.getSite() != null)
            mission.setSite(missionDto.getSite());
        if (missionDto.getStartTerm() != null)
            mission.setStartTerm(missionDto.getStartTerm());
        if (missionDto.getEndTerm() != null)
            mission.setEndTerm(missionDto.getEndTerm());
        if (missionDto.getSeason() != null)
            mission.setSeason(missionDto.getSeason());
        if (missionDto.getTestimony() != null)
            mission.setTestimony(missionDto.getTestimony());

        try {
            missionJpaRepository.save(mission);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_MISSION, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // 선교 삭제하기
    public StatusCode deleteMission(long id, UUID uuid) {
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;

        try {
            deleteAllMissionImages(id, uuid); // 선교 삭제에 따라 #{id} 선교의 모든 이미지 삭제하기
            missionJpaRepository.deleteById(id);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // 모든 선교의 생성자의 uuid 가져오기
    public Page<UUID> getAllAuthorUuid(Pageable pageable) {
        return missionJpaRepository.findAuthorUuid(pageable);
    }

    // 선교 사진 올리기
    public StatusCode uploadMissionImages(List<MultipartFile> files, long id, UUID uuid) {
        // #{id} 선교 게시글을 수정할 권한 정보 확인하기
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // 선교의 이미지 개수를 0으로 설정하기
        mission.setImageCount((short) 0);

        for (short i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            try {
                // 확장자 추출하기
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains("."))
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));

                // 파일 이름 설정하기
                String fileName = String.format("%d-%d", id, i);
                String path = "mission/" + fileName;

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
            } catch (Exception e) {
                deleteAllMissionImages(id, uuid);
                LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
                return StatusCode.SOMETHING_WENT_WRONG;
            }
        }

        mission.setImageCount((short) files.size());
        try {
            missionJpaRepository.save(mission);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
        }
        return StatusCode.NO_ERROR;
    }

    public StatusCode deleteAllMissionImages(long id, UUID uuid) {
        // #{id} 선교가 uuid가 {uuid}인 사용자에 의해 생성되었는지 확인하기
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // 모든 이미지 삭제하기
        short imageCount = mission.getImageCount();
        for (short i = 0; i < imageCount; i++) {
            try {
                // 삭제하기
                deleteSpecificMissionImage(id, i);
            } catch (Exception e) {
                LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION_IMAGE, LogUtil.makeExceptionKV(e));
                return StatusCode.SOMETHING_WENT_WRONG;
            }
        }

        // 스토리지에 이미지가 없으므로 image_count를 0으로 설정하기
        mission.setImageCount((short) 0);
        try {
            missionJpaRepository.save(mission);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE, LogUtil.makeExceptionKV(e));
        }
        return StatusCode.NO_ERROR;
    }
}
