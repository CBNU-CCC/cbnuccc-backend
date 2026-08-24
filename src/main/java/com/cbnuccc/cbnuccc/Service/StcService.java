package com.cbnuccc.cbnuccc.Service;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.stereotype.Service;
import com.cbnuccc.cbnuccc.Dto.StcDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.Stc;
import com.cbnuccc.cbnuccc.Model.StcTopic;
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
                stc.getComment());
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

    // STC 정보 액셀 다운로드
    @Transactional
    public void downloadStc(HttpServletResponse response) {
        try {
            // 엑셀 워크북 생성 (.xlsx)
            Workbook workbook = new XSSFWorkbook();

            // 현재 존재하는 항목 개수 (항목 열은 이 개수만큼 동적으로 생성됨)
            int topicCount = stcTopicJpaRepository.findMaxTopicNumber().orElse((short) 0);
            int commentColumnIndex = 3 + topicCount;

            // 학년별 시트 생성
            for (short grade = 1; grade <= 5; grade++) {
                Sheet sheet = workbook.createSheet(grade == 5 ? "기타" : grade + "학년");

                // 헤더(제목 행) 생성
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("학년");
                headerRow.createCell(1).setCellValue("이름");
                headerRow.createCell(2).setCellValue("기록일");
                for (int topicNumber = 1; topicNumber <= topicCount; topicNumber++) {
                    headerRow.createCell(2 + topicNumber).setCellValue("항목" + topicNumber);
                }
                headerRow.createCell(commentColumnIndex).setCellValue("의견");

                // 데이터 행 생성
                int rowNumber = 1;
                List<LocalDate> dates = stcJpaRepository.findAllDates();

                Page<UUID> authors = stcJpaRepository.findAuthorUuidByGrade(Pageable.unpaged(), grade);
                for (UUID authorUuid : authors) {
                    // 가독성을 위해 한 줄 띄우기
                    rowNumber++;

                    // 각 참여자의 학년 및 이름 정보 가져오기
                    Optional<MyUser> _author = userJpaRepository.findByUuid(authorUuid);
                    if (_author.isEmpty())
                        continue;
                    MyUser author = _author.get();

                    // 참여자 헤더
                    Row userHeaderRow = sheet.createRow(rowNumber++);
                    userHeaderRow.createCell(0).setCellValue(author.getGrade()); // 학년
                    userHeaderRow.createCell(1).setCellValue(author.getName()); // 이름
                    userHeaderRow.createCell(2).setCellValue("소계");
                    for (short topicNumber = 1; topicNumber <= topicCount; topicNumber++) {
                        userHeaderRow.createCell(2 + topicNumber)
                                .setCellValue(
                                        stcTopicJpaRepository
                                                .countByStcAuthorUuidAndTopicNumberAndCompletionGreaterThanEqual(
                                                        authorUuid, topicNumber, (short) 1));
                    }

                    // stc 활동에 따른 추가 행 삽입
                    for (LocalDate date : dates) {
                        // 해당 기록일 확인
                        Optional<Stc> _stc = stcJpaRepository.findByAuthorUuidAndRecordDate(authorUuid, date);

                        // 값 기록 안 했다면 FALSE로 간주
                        Map<Short, Short> completionByTopicNumber = Map.of();
                        String comment = "";
                        if (_stc.isPresent()) {
                            Stc stc = _stc.get();
                            completionByTopicNumber = stc.getTopics().stream()
                                    .collect(Collectors.toMap(stcTopic -> stcTopic.getTopicNumber(),
                                            stcTopic -> stcTopic.getCompletion()));

                            // 의견 존재하면 병기
                            String _comment = stc.getComment();
                            comment = _comment == null ? "" : _comment;
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
                        r.createCell(2).setCellValue(date.toString());

                        for (short topicNumber = 1; topicNumber <= topicCount; topicNumber++) {
                            short completion = completionByTopicNumber.getOrDefault(topicNumber, (short) 0);
                            Cell topicCell = r.createCell(2 + topicNumber);
                            topicCell.setCellValue(completion);
                            topicCell.setCellStyle(completion >= 1 ? trueStyle : falseStyle);
                        }

                        r.createCell(commentColumnIndex).setCellValue(comment);
                    }
                }

                sheet.autoSizeColumn(2); // 기록일 컬럼에 대해 크기 조정
            }

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
}
