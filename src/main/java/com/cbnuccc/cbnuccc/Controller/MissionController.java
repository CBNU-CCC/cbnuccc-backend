package com.cbnuccc.cbnuccc.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MissionController {
    private final UserService userService;
    private final MissionService missionService;

    // get all missions.
    @GetMapping("/mission")
    public ResponseEntity<?> getMission(Pageable pageable) {
        Page<MissionDto> result = missionService.getAllMissions(pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, LogUtil.makeCountKV(result.getSize()));
        return ResponseEntity.ok(result);
    }

    // get a specific mission.
    @GetMapping("/mission/{id}")
    public ResponseEntity<?> getSpecificMission(@PathVariable("id") int id) {
        DataWithStatusCode<MissionDto> result = missionService.getSpecificMission(id);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.GET_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(result.data());
    }

    // get my missions.
    @GetMapping("/my-mission")
    public ResponseEntity<?> getMyMissions(Authentication authentication, Pageable pageable) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        Page<MissionDto> missions = missionService.getAllMyMissions(uuid, pageable);

        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, LogUtil.makeCountKV(missions.getSize()));
        return ResponseEntity.ok(missions);
    }

    // create a mission.
    @PostMapping("/mission")
    public ResponseEntity<?> createMission(Authentication authentication, @RequestBody MissionDto missionDto) {
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

    // update given mission.
    @PatchMapping("/mission/{id}")
    public ResponseEntity<?> updateMission(Authentication authentication, @PathVariable("id") int id,
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

    // delete given mission.
    @DeleteMapping("/mission/{id}")
    public ResponseEntity<?> deleteMission(Authentication authentication, @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // being deleted...
        DataWithStatusCode<MissionDto> result = missionService.getSpecificMission(id);
        StatusCode code = result.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }
        MissionDto deletedMission = result.data();

        // delete it
        code = missionService.deleteMission(id, uuid);
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }

        LogUtil.printBasicInfoLog(LogHeader.DELETE_MISSION, LogUtil.makeIdKV(id));
        return ResponseEntity.ok(deletedMission);
    }

    // upload mission's images
    @PostMapping("/mission-image/{id}")
    public ResponseEntity<?> uploadMissionImage(Authentication authentication,
            @RequestParam("files") List<MultipartFile> _files,
            @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // compress images
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

        // check size of all files
        long sumOfImageSizes = 0;
        for (MultipartFile file : files)
            sumOfImageSizes += file.getSize();
        if (sumOfImageSizes > 1 * 1024 * 1024) { // 1MB
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_MISSION_IMAGE, (Object[]) null);
            return StatusCode.EXCEED_1MB.makeErrorResponseEntity();
        }

        // save files
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

    // delete all images of #{id} mission.
    @DeleteMapping("/mission-image/{id}")
    public ResponseEntity<?> deleteAllMissionImage(Authentication authentication, @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // get information which being deleted
        DataWithStatusCode<MissionDto> _mission = missionService.getSpecificMission(id);
        StatusCode code = _mission.code();
        if (code.checkIsError()) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION_IMAGE, LogUtil.makeStatusCodeMessageKV(code));
            return code.makeErrorResponseEntity();
        }
        short originalImageCount = _mission.data().getImageCount();

        // delete all images
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
