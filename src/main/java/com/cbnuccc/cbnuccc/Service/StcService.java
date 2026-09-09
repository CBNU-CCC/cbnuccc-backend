package com.cbnuccc.cbnuccc.Service;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.ReviewSoonInfoDto;
import com.cbnuccc.cbnuccc.Dto.StcDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.ReviewSoonInfo;
import com.cbnuccc.cbnuccc.Model.Stc;
import com.cbnuccc.cbnuccc.Model.StcTopic;
import com.cbnuccc.cbnuccc.Repository.ReviewSoonInfoJpaRepository;
import com.cbnuccc.cbnuccc.Repository.StcJpaRepository;
import com.cbnuccc.cbnuccc.Repository.StcTopicJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StcService {
    private final UserJpaRepository userJpaRepository;
    private final StcJpaRepository stcJpaRepository;
    private final StcTopicJpaRepository stcTopicJpaRepository;
    private final ReviewSoonInfoJpaRepository reviewSoonInfoJpaRepository;

    // Stc를 StcDto로 변환하기
    private StcDto stcToStcDto(Stc stc) {
        UUID authorUuid = stc.getAuthor().getUuid(); // 작성자의 uuid 가져오기

        // stc_topic 테이블에 흩어진 항목별 이수 여부를 하나의 short 리스트로 합치기 (테이블 분리를 감춤)
        List<Short> topics = stc.getTopics().stream()
                .map(stcTopic -> stcTopic.getCompletion())
                .collect(Collectors.toList());

        return new StcDto(
                stc.getId(),
                authorUuid,
                stc.getRecordDate(),
                topics,
                stc.getComment(),
                stc.getWeeklyLife(),
                stc.getPrayerRequest(),
                stc.getReview());
    }

    // 내 모든 STC 정보 가져오기
    @Transactional
    public Page<StcDto> getAllMyStcs(UUID uuid, Pageable pageable) {
        Page<Stc> stcs = stcJpaRepository.findAllByAuthorUuid(uuid, pageable);
        return stcs.map(stc -> stcToStcDto(stc));
    }

    // 내 특정 STC 정보 가져오기
    @Transactional
    public DataWithStatusCode<StcDto> getMySpecificStc(long id, UUID uuid) {
        Optional<Stc> _stc = stcJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_stc.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_STC_FOUND, null);
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, stcToStcDto(_stc.get()));
    }

    // 특정인 및 특정일의 STC 정보 조회
    @Transactional
    public DataWithStatusCode<StcDto> getSpecificStc(UUID uuid, LocalDate recordDate) {
        // stc 정보 조회
        Optional<Stc> _stc = stcJpaRepository.findByAuthorUuidAndRecordDate(uuid, recordDate);
        if (_stc.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_STC_FOUND, null);
        Stc stc = _stc.get();

        // 변환 후 반환
        StcDto stcDto = stcToStcDto(stc);
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, stcDto);
    }

    // 주어진 uuid의 사용자가 주어진 점검순의 대표이면 true, 아니면 false
    // 단, 점검순이 존재하지 않는 경우 false
    @Transactional
    public boolean isReviewSoonRepresentativeOf(UUID uuid, ReviewSoonInfoDto reviewSoon) {
        if (reviewSoon == null)
            return false;

        Optional<ReviewSoonInfo> _reviewSoonInfo = reviewSoonInfoJpaRepository.findById(reviewSoon.getId());
        if (_reviewSoonInfo.isEmpty())
            return false;

        // 일치하는지 확인 후 boolean 값 반환
        ReviewSoonInfo reviewSoonInfo = _reviewSoonInfo.get();
        return reviewSoonInfo.getRepresentative().getUuid().equals(uuid);
    }

    // 점검순 인원에 대한 점검 한 마디 작성
    @Transactional
    public StatusCode createMessageToReviewSoonPerson(UUID to, LocalDate recordDate, String message) {
        // stc 정보 조회
        Optional<Stc> _stc = stcJpaRepository.findByAuthorUuidAndRecordDate(to, recordDate);
        if (_stc.isEmpty())
            return StatusCode.NO_STC_FOUND;
        Stc stc = _stc.get();

        // 점검 메시지 작성
        stc.setReview(message);

        // 저장
        stcJpaRepository.save(stc);
        return StatusCode.NO_ERROR;
    }

    // STC 정보 생성하기
    @Transactional
    public DataWithStatusCode<StcDto> createStc(StcDto stcDto, UUID uuid) {
        // 작성자 정보 찾기
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_USER_FOUND, null);
        MyUser user = _user.get();

        // STC 인스턴스 생성하기
        Stc stc = new Stc();
        stc.setAuthor(user);
        stc.setRecordDate(stcDto.getRecordDate());
        stc.setComment(stcDto.getComment());
        stc.setWeeklyLife(stcDto.getWeeklyLife());
        stc.setPrayerRequest(stcDto.getPrayerRequest());

        // 리스트의 인덱스 + 1을 항목 번호로 하여 stc_topic 행 생성하기
        List<Short> completions = stcDto.getTopics();
        List<StcTopic> topics = new ArrayList<>();
        for (int i = 0; i < completions.size(); i++) {
            StcTopic topic = new StcTopic();
            topic.setStc(stc);
            topic.setTopicNumber((short) (i + 1));
            topic.setCompletion(completions.get(i));
            topics.add(topic);
        }
        stc.setTopics(topics);

        try {
            // 저장하기 (stc_topic도 함께 저장됨)
            Stc createdStc = stcJpaRepository.save(stc);
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, stcToStcDto(createdStc));
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_STC, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // 학년 문자열 변환 (없으면 빈칸, 5학년은 실존하지 않아 기타로 표시)
    private String gradeToString(Short grade) {
        if (grade == null)
            return "";
        if (grade == 5)
            return "기타";
        return grade + "학년";
    }

    // 엑셀 시트명 제약(31자, 금지문자, 중복)에 맞게 이름 정제
    private String sanitizeSheetName(String rawName, Set<String> usedSheetNames) {
        String name = rawName == null || rawName.isBlank() ? "이름없음" : rawName;
        name = name.replaceAll("[\\\\/?*\\[\\]:]", "_"); // 금지 문자 치환
        if (name.length() > 31)
            name = name.substring(0, 31);

        // 이름이 중복되면 뒤에 (2), (3)... 붙이기
        String base = name;
        int suffix = 2;
        while (!usedSheetNames.add(name)) {
            String suffixString = "(" + suffix + ")";
            int cut = Math.max(0, 31 - suffixString.length());
            name = base.substring(0, Math.min(base.length(), cut)) + suffixString;
            suffix++;
        }
        return name;
    }

    // 주어진 참여자 목록으로 하나의 시트를 채우기
    private void fillSheet(Workbook workbook, Sheet sheet, Page<UUID> authors, List<LocalDate> dates,
            int topicCount) {
        int gradeColumnIndex = 1;
        int recordDateColumnIndex = 3;
        int weeklyLifeColumnIndex = 4 + topicCount;
        int prayerRequestColumnIndex = weeklyLifeColumnIndex + 1;
        int commentColumnIndex = prayerRequestColumnIndex + 1;
        int reviewColumnIndex = commentColumnIndex + 1;

        // 헤더(제목 행) 생성
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("직분");
        headerRow.createCell(gradeColumnIndex).setCellValue("학년");
        headerRow.createCell(2).setCellValue("이름");
        headerRow.createCell(recordDateColumnIndex).setCellValue("기록일");
        for (int topicNumber = 1; topicNumber <= topicCount; topicNumber++) {
            headerRow.createCell(3 + topicNumber).setCellValue("항목" + topicNumber);
        }
        headerRow.createCell(weeklyLifeColumnIndex).setCellValue("일주일의 삶");
        headerRow.createCell(prayerRequestColumnIndex).setCellValue("기도제목");
        headerRow.createCell(commentColumnIndex).setCellValue("소감");
        headerRow.createCell(reviewColumnIndex).setCellValue("점검 순장의 한 마디");

        // 데이터 행 생성
        int rowNumber = 1;
        for (UUID authorUuid : authors) {
            // 가독성을 위해 한 줄 띄우기
            rowNumber++;

            // 각 참여자의 학년 및 이름 정보 가져오기
            Optional<MyUser> _author = userJpaRepository.findByUuid(authorUuid);
            if (_author.isEmpty())
                continue;
            MyUser author = _author.get();

            // 직분 매핑
            String rankString = "";
            switch (author.getRank()) {
                case 0:
                    rankString = "순원";
                    break;
                case 1:
                    rankString = "순장";
                    break;
                case 2:
                    rankString = "사역팀";
                    break;
                case 3:
                    rankString = "나사렛";
                    break;
                case 4:
                    rankString = "간사";
                    break;

                default:
                    break;
            }

            // 참여자 헤더
            Row userHeaderRow = sheet.createRow(rowNumber++);
            userHeaderRow.createCell(0).setCellValue(rankString); // 직분
            userHeaderRow.createCell(gradeColumnIndex).setCellValue(gradeToString(author.getGrade())); // 학년
            userHeaderRow.createCell(2).setCellValue(author.getName()); // 이름
            userHeaderRow.createCell(recordDateColumnIndex).setCellValue("소계");
            for (short topicNumber = 1; topicNumber <= topicCount; topicNumber++) {
                userHeaderRow.createCell(3 + topicNumber)
                        .setCellValue(
                                stcTopicJpaRepository
                                        .sumCompletionByStcAuthorUuidAndTopicNumber(
                                                authorUuid, topicNumber));
            }

            // stc 활동에 따른 추가 행 삽입
            for (LocalDate date : dates) {
                // 해당 기록일 확인
                Optional<Stc> _stc = stcJpaRepository.findByAuthorUuidAndRecordDate(authorUuid, date);

                // 값 기록 안 했다면 FALSE로 간주
                Map<Short, Short> completionByTopicNumber = Map.of();
                String weeklyLife = "", prayerRequest = "", comment = "", review = "";
                if (_stc.isPresent()) {
                    Stc stc = _stc.get();
                    completionByTopicNumber = stc.getTopics().stream()
                            .collect(Collectors.toMap(stcTopic -> stcTopic.getTopicNumber(),
                                    stcTopic -> stcTopic.getCompletion()));

                    // 일주읠 삶, 기도제목, 소감, 점검 순장의 한 마디 존재하면 병기
                    String _weeklyLife = stc.getWeeklyLife();
                    weeklyLife = _weeklyLife == null ? "" : _weeklyLife;
                    String _prayerRequest = stc.getPrayerRequest();
                    prayerRequest = _prayerRequest == null ? "" : _prayerRequest;
                    String _comment = stc.getComment();
                    comment = _comment == null ? "" : _comment;
                    String _review = stc.getReview();
                    review = _review == null ? "" : _review;
                }

                // TRUE / FLASE 색깔 스타일 입히기
                CellStyle falseStyle = workbook.createCellStyle();
                CellStyle trueStyle = workbook.createCellStyle();

                // FALSE → 연한 빨강
                falseStyle.setFillForegroundColor(
                        new XSSFColor(new Color(255, 228, 225), new DefaultIndexedColorMap()));
                falseStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // TRUE → 연한 파랑
                trueStyle.setFillForegroundColor(
                        new XSSFColor(new Color(224, 242, 254), new DefaultIndexedColorMap()));
                trueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // 기록
                Row r = sheet.createRow(rowNumber++);
                r.createCell(recordDateColumnIndex).setCellValue(date.toString());

                for (short topicNumber = 1; topicNumber <= topicCount; topicNumber++) {
                    short completion = completionByTopicNumber.getOrDefault(topicNumber, (short) 0);
                    Cell topicCell = r.createCell(3 + topicNumber);
                    topicCell.setCellValue(completion);
                    topicCell.setCellStyle(completion >= 1 ? trueStyle : falseStyle);
                }

                r.createCell(weeklyLifeColumnIndex).setCellValue(weeklyLife);
                r.createCell(prayerRequestColumnIndex).setCellValue(prayerRequest);
                r.createCell(commentColumnIndex).setCellValue(comment);
                r.createCell(reviewColumnIndex).setCellValue(review);
            }
        }

        sheet.autoSizeColumn(recordDateColumnIndex); // 기록일 컬럼에 대해 크기 조정
        sheet.autoSizeColumn(weeklyLifeColumnIndex); // 일주일 삶 컬럼에 대해 크기 조정
        sheet.autoSizeColumn(prayerRequestColumnIndex); // 기도제목 컬럼에 대해 크기 조정
        sheet.autoSizeColumn(commentColumnIndex); // 소감 컬럼에 대해 크기 조정
        sheet.autoSizeColumn(reviewColumnIndex); // 점검 순장의 한 마디 컬럼에 대해 크기 조정
    }

    // STC 정보 액셀 다운로드
    @Transactional
    public void downloadStc(HttpServletResponse response) {
        try {
            // 엑셀 워크북 생성 (.xlsx)
            Workbook workbook = new XSSFWorkbook();

            // 현재 존재하는 항목 개수 (항목 열은 이 개수만큼 동적으로 생성됨)
            int topicCount = stcTopicJpaRepository.findMaxTopicNumber().orElse((short) 0);

            List<LocalDate> dates = stcJpaRepository.findAllDates();
            Set<String> usedSheetNames = new HashSet<>();

            // 점검순별 시트 생성 (점검순 목록은 review_soon_info 테이블에서 동적으로 조회)
            List<ReviewSoonInfo> reviewSoonInfoList = reviewSoonInfoJpaRepository
                    .findAll(Sort.by(Sort.Direction.ASC, "id"));
            for (ReviewSoonInfo reviewSoonInfo : reviewSoonInfoList) {
                Sheet sheet = workbook.createSheet(sanitizeSheetName(reviewSoonInfo.getName(), usedSheetNames));
                Page<UUID> authors = stcJpaRepository.findAuthorUuidByAffiliatedReviewSoonId(
                        Pageable.unpaged(), reviewSoonInfo.getId());
                fillSheet(workbook, sheet, authors, dates, topicCount);
            }

            // 소속된 점검순이 없는 사용자를 모은 기타 시트 생성
            Sheet etcSheet = workbook.createSheet(sanitizeSheetName("기타", usedSheetNames));
            Page<UUID> etcAuthors = stcJpaRepository.findAuthorUuidByAffiliatedReviewSoonIsNull(Pageable.unpaged());
            fillSheet(workbook, etcSheet, etcAuthors, dates, topicCount);

            // HTTP 응답 설정
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=STC_excel.xlsx");

            // 스트림으로 엑셀 파일 출력
            workbook.write(response.getOutputStream());
            workbook.close();

            // 로그 출력
            LogUtil.printBasicInfoLog(LogHeader.DOWNLOAD_STC, (Object[]) null);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DOWNLOAD_STC, LogUtil.makeExceptionKV(e));
        }
    }

    @Transactional
    public DataWithStatusCode<ReviewSoonInfoDto> getAffiliatedReviewSoon(UUID uuid) {
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_REVIEW_SOON,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_USER_FOUND));
            return new DataWithStatusCode<>(StatusCode.NO_USER_FOUND, null);
        }
        MyUser user = _user.get();
        ReviewSoonInfo reviewSoonInfo = user.getAffiliatedReviewSoon();
        if (reviewSoonInfo == null) {
            LogUtil.printBasicWarnLog(LogHeader.GET_REVIEW_SOON,
                    LogUtil.makeStatusCodeMessageKV(StatusCode.NO_REVIEW_SOON_FOUND));
            return new DataWithStatusCode<>(StatusCode.NO_REVIEW_SOON_FOUND, null);
        }

        // 본인(uuid)의 소속 점검순을 조회하는 것이므로, 대표 여부도 같은 uuid로 확인함
        MyUser representative = reviewSoonInfo.getRepresentative();
        boolean isRepresentative = representative != null && representative.getUuid().equals(uuid);

        return new DataWithStatusCode<>(StatusCode.NO_ERROR,
                new ReviewSoonInfoDto(reviewSoonInfo.getId(), reviewSoonInfo.getName(), isRepresentative));
    }

    // 본인이 소속된 모든 사용자의 uuid 반환
    @Transactional
    public Page<UUID> getAllUsersWhoBelongToAffiliatedReviewSoon(UUID me, Pageable pageable) {
        DataWithStatusCode<ReviewSoonInfoDto> reviewSoonInfo = getAffiliatedReviewSoon(me);
        if (reviewSoonInfo.code().checkIsError())
            return Page.empty(); // 소속된 순에 이상이 있을 경우 없는 페이지 리턴

        Page<UUID> uuids = userJpaRepository.findAllAffiliatedReviewSoonUsersUuid(reviewSoonInfo.data().getId(),
                pageable);
        return uuids;
    }
}
