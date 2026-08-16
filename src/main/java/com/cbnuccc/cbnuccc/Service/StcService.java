package com.cbnuccc.cbnuccc.Service;

import java.awt.Color;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.cbnuccc.cbnuccc.Repository.StcJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StcService {
    private final UserJpaRepository userJpaRepository;
    private final StcJpaRepository stcJpaRepository;

    // Stc를 StcDto로 변환하기
    private StcDto stcToStcDto(Stc stc) {
        UUID authorUuid = stcJpaRepository.findAuthorUuidByStcId(stc.getId()).get(); // 작성자의 uuid 가져오기
        return new StcDto(
                stc.getId(),
                authorUuid,
                stc.getRecordDate(),
                stc.getTopic1(),
                stc.getTopic2(),
                stc.getTopic3(),
                stc.getComment());
    }

    // 내 모든 STC 정보 가져오기
    public Page<StcDto> getAllMyStcs(UUID uuid, Pageable pageable) {
        Page<Stc> stcs = stcJpaRepository.findAllByAuthorUuid(uuid, pageable);
        return stcs.map(stc -> stcToStcDto(stc));
    }

    // 내 특정 STC 정보 가져오기
    public DataWithStatusCode<StcDto> getMySpecificStc(long id, UUID uuid) {
        Optional<Stc> _stc = stcJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_stc.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_STC_FOUND, null);
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, stcToStcDto(_stc.get()));
    }

    // STC 정보 생성하기
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
        stc.setTopic1(stcDto.getTopic1());
        stc.setTopic2(stcDto.getTopic2());
        stc.setTopic3(stcDto.getTopic3());
        stc.setComment(stcDto.getComment());

        try {
            // 저장하기
            Stc createdStc = stcJpaRepository.save(stc);
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, stcToStcDto(createdStc));
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_STC, LogUtil.makeExceptionKV(e));
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    public void downloadStc(HttpServletResponse response) {
        try {
            // 엑셀 워크북 생성 (.xlsx)
            Workbook workbook = new XSSFWorkbook();

            // 학년별 시트 생성
            for (short grade = 1; grade <= 5; grade++) {
                Sheet sheet = workbook.createSheet(grade == 5 ? "기타" : grade + "학년");

                // 헤더(제목 행) 생성
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("학년");
                headerRow.createCell(1).setCellValue("이름");
                headerRow.createCell(2).setCellValue("기록일");
                headerRow.createCell(3).setCellValue("항목1");
                headerRow.createCell(4).setCellValue("항목2");
                headerRow.createCell(5).setCellValue("항목3");
                headerRow.createCell(6).setCellValue("의견");

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
                    userHeaderRow.createCell(3)
                            .setCellValue(stcJpaRepository.countByAuthorUuidAndTopic1True(authorUuid));
                    userHeaderRow.createCell(4)
                            .setCellValue(stcJpaRepository.countByAuthorUuidAndTopic2True(authorUuid));
                    userHeaderRow.createCell(5)
                            .setCellValue(stcJpaRepository.countByAuthorUuidAndTopic3True(authorUuid));

                    // stc 활동에 따른 추가 행 삽입
                    for (LocalDate date : dates) {
                        // 해당 기록일 확인
                        Optional<Stc> _stc = stcJpaRepository.findByAuthorUuidAndRecordDate(authorUuid, date);

                        // 값 기록 안 했다면 FALSE로 간주
                        boolean topic1 = false, topic2 = false, topic3 = false;
                        String comment = "";
                        if (_stc.isPresent()) {
                            Stc stc = _stc.get();
                            topic1 = stc.getTopic1();
                            topic2 = stc.getTopic2();
                            topic3 = stc.getTopic3();

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

                        Cell topic1Cell = r.createCell(3);
                        topic1Cell.setCellValue(topic1);
                        topic1Cell.setCellStyle(topic1 ? trueStyle : falseStyle);

                        Cell topic2Cell = r.createCell(4);
                        topic2Cell.setCellValue(topic2);
                        topic2Cell.setCellStyle(topic2 ? trueStyle : falseStyle);

                        Cell topic3Cell = r.createCell(5);
                        topic3Cell.setCellValue(topic3);
                        topic3Cell.setCellStyle(topic3 ? trueStyle : falseStyle);

                        r.createCell(6).setCellValue(comment);
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

            LogUtil.printBasicInfoLog(LogHeader.DOWNLOAD_STC, (Object[]) null);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DOWNLOAD_STC, LogUtil.makeExceptionKV(e));
        }
    }
}
