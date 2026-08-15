package com.cbnuccc.cbnuccc.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cbnuccc.cbnuccc.Dto.MissionDto;
import com.cbnuccc.cbnuccc.Service.MissionService;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.ImageUtil;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.PaginationUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name="Mission Controller", description="선교 및 기도편지 관련 기능")
@RestController
@RequiredArgsConstructor
public class MissionController {
    private final UserService userService;
    private final MissionService missionService;

    @Operation(summary = "전체 선교 목록 조회", description = "등록된 모든 선교 목록을 가져옵니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "선교 목록 조회 성공")
    })
    @GetMapping("/mission")
    public ResponseEntity<?> getMission(@ParameterObject Pageable pageable) {
        Page<MissionDto> result = missionService.getAllMissions(pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION,
                LogUtil.makeCountKV(result.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(result));
    }

    @Operation(summary = "단일 선교 조회", description = "ID에 해당하는 특정 선교 게시글을 가져옵니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "선교 상세 조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 ID의 선교를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/mission/{id}")
    public ResponseEntity<?> getSpecificMission(
            @Parameter(description = "조회할 선교 ID", example = "1") @PathVariable("id") int id) {
        DataWithStatusCode<MissionDto> result = missionService.getSpecificMission(id);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }

    @Operation(summary = "내 선교 목록 조회", description = "본인이 작성한 선교 목록을 조회한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내 선교 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 미제공 또는 만료)", content = @Content)
    })
    @GetMapping("/my-mission")
    public ResponseEntity<?> getMyMissions(
            @Parameter(hidden = true) Authentication authentication,
            @ParameterObject Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<MissionDto> missions = missionService.getAllMyMissions(uuid, pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION,
                LogUtil.makeCountKV(missions.getNumberOfElements()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(missions));
    }

    @Operation(summary = "선교 게시글 생성", description = "새로운 선교글을 작성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "선교 생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/mission")
    public ResponseEntity<?> createMission(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody MissionDto missionDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<MissionDto> result = missionService.createMission(missionDto, uuid);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        MissionDto createdMissionDto = result.data();
        LogUtil.printBasicInfoLog(LogHeader.CREATE_MISSION, LogUtil.makeIdKV(result.data().getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMissionDto);
    }

    @Operation(summary = "선교 수정", description = "작성한 선교 게시글을 수정한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "선교 수정 성공"),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음", content = @Content),
        @ApiResponse(responseCode = "404", description = "선교 게시글 없음", content = @Content)
    })
    @PatchMapping("/mission/{id}")
    public ResponseEntity<?> updateMission(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "수정할 선교 ID", example = "1") @PathVariable("id") int id,
            @RequestBody MissionDto missionDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = missionService.updateMission(id, uuid, missionDto);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.UPDATE_MISSION, LogUtil.makeIdKV(id));
        return getSpecificMission(id);
    }

    @Operation(summary = "선교 삭제", description = "선교 게시글을 삭제합니다.")
    @DeleteMapping("/mission/{id}")
    public ResponseEntity<?> deleteMission(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "삭제할 선교 ID", example = "1") @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // 삭제할 선교 내용 가져오기
        DataWithStatusCode<MissionDto> result = missionService.getSpecificMission(id);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }
        MissionDto deletedMission = result.data();

        // 삭제하기
        code = missionService.deleteMission(id, uuid);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.DELETE_MISSION, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(deletedMission);
    }

    @Operation(summary = "모든 선교 작성자의 uuid 가져오기",description = "선교를 작성한 모든 사용자의 uuid를 조회한다.")
    @GetMapping("/mission/author")
    public ResponseEntity<?> getAllAuthorUuid(@ParameterObject Pageable pageable) {
        Page<UUID> uuids = missionService.getAllAuthorUuid(pageable);
        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION_AUTHOR,
                LogUtil.makeCountKV(uuids.getNumber()),
                LogUtil.makePageNumberKV(pageable),
                LogUtil.makePageSizeKV(pageable));
        return ResponseEntity.ok(PaginationUtil.makePaginationMap(uuids));
    }

    @Operation(summary = "선교 이미지 업로드", description = "선교 게시글에 선교 기도편지 파일들을 업로드합니다.(2MB 제한)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
        @ApiResponse(responseCode = "400", description = "용량(2MB) 초과 또는 압축 실패", content = @Content),
        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PostMapping("/mission-image/{id}")
    public ResponseEntity<?> uploadMissionImageuploadMissionImage(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "업로드할 이미지 파일 리스트", required = true) 
            @RequestParam("files") List<MultipartFile> _files,
            @Parameter(description = "선교 ID", example = "1") @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // 이미지 압축하기
        List<MultipartFile> files = new ArrayList<>();
        for (MultipartFile file : _files) {
            DataWithStatusCode<MultipartFile> data = ImageUtil.makeImageLowQuality(file);
            StatusCode code = data.code();
            if (code.checkIsError()) {
                LogUtil.printBasicWarnLog(LogHeader.UPLOAD_MISSION_IMAGE, LogUtil.makeStatusCodeMessageKV(code));
                return code.makeErrorResponseEntity();
            }
            files.add(data.data());
        }

        // 모든 이미지 파일의 용량 확인하기
        long sumOfImageSizes = 0;
        for (MultipartFile file : files)
            sumOfImageSizes += file.getSize();
        if (sumOfImageSizes > 2 * 1024 * 1024) { // 2MB
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_MISSION_IMAGE, (Object[]) null);
            return StatusCode.EXCEED_2MB.makeErrorResponseEntity();
        }

        // 이미지 저장하기
        StatusCode code = missionService.uploadMissionImages(files, id, uuid);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_MISSION_IMAGE, (Object[]) null);
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(
                LogHeader.UPLOAD_MISSION_IMAGE,
                LogUtil.makeIdKV(id),
                LogUtil.makeCountKV(files.size()));
        return StatusCode.NO_ERROR.makeErrorResponseEntity();
    }

    @Operation(summary = "선교 사진 전체 삭제", description="주어진 아이디의 선교 사진 모두 삭제하기")
    @DeleteMapping("/mission-image/{id}")
    public ResponseEntity<?> deleteAllMissionImage(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "선교 ID", example = "1") @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // 삭제될 정보 가져오고 확인하기
        DataWithStatusCode<MissionDto> _mission = missionService.getSpecificMission(id);
        StatusCode code = _mission.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION_IMAGE, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }
        short originalImageCount = _mission.data().getImageCount();

        // 모든 이미지 삭제하기
        code = missionService.deleteAllMissionImages(id, uuid);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION_IMAGE, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(
                LogHeader.DELETE_MISSION_IMAGE,
                LogUtil.makeIdKV(id),
                LogUtil.makeCountKV(originalImageCount));
        return code.makeErrorResponseEntity();
    }
}
