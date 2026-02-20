package com.cbnuccc.cbnuccc.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<?> getMission() {
        List<MissionDto> result = missionService.getAllMissions();

        // print log
        String message = String.format("successfully get %d all missions", result.size());
        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, message, null);
        return ResponseEntity.ok(result);
    }

    // get a specific mission.
    @GetMapping("/mission/{id}")
    public ResponseEntity<?> getSpecificMission(@PathVariable("id") int id) {
        DataWithStatusCode<MissionDto> result = missionService.getSpecificMission(id);
        if (result.code().checkIsError())
            return result.code().makeErrorResponseEntityAndPrintLog(LogHeader.GET_MISSION, null);

        // print log
        String message = String.format("successfully get a specific mission #%d.", result.data().getId());
        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, message, null);

        return ResponseEntity.ok(result.data());
    }

    // get my missions.
    @GetMapping("/my-mission")
    public ResponseEntity<?> getMyMissions(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        List<MissionDto> missions = missionService.getAllMyMissions(uuid);

        // print log
        String message = String.format("successfully get %d all my missions", missions.size());
        LogUtil.printBasicInfoLog(LogHeader.GET_MISSION, message, uuid);
        return ResponseEntity.ok(missions);
    }

    // create a mission.
    @PostMapping("/mission")
    public ResponseEntity<?> createMission(Authentication authentication, @RequestBody MissionDto missionDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<MissionDto> result = missionService.createMission(missionDto, uuid);
        if (result.code().checkIsError())
            return result.code().makeErrorResponseEntityAndPrintLog(LogHeader.CREATE_MISSION, uuid);

        MissionDto createdMissionDto = result.data();
        return ResponseEntity.ok(createdMissionDto);
    }

    // update given mission.
    @PatchMapping("/mission/{id}")
    public ResponseEntity<?> updateMission(Authentication authentication, @PathVariable("id") int id,
            @RequestBody MissionDto missionDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = missionService.updateMission(id, uuid, missionDto);
        if (code.checkIsError())
            return code.makeErrorResponseEntityAndPrintLog(LogHeader.UPDATE_MISSION, uuid);
        return getSpecificMission(id);
    }

    // delete given mission.
    @DeleteMapping("/mission/{id}")
    public ResponseEntity<?> deleteMission(Authentication authentication, @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);

        // being deleted...
        DataWithStatusCode<MissionDto> result = missionService.getSpecificMission(id);
        if (result.code().checkIsError())
            return result.code().makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_MISSION, uuid);
        MissionDto deletedMission = result.data();

        // delete it
        StatusCode code = missionService.deleteMission(id, uuid);
        if (code.checkIsError())
            return code.makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_MISSION, uuid);

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
            if (data.code().checkIsError())
                return data.code().makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_MISSION_IMAGE, uuid);
            files.add(data.data());
        }

        // check size of all files
        long sumOfImageSizes = 0;
        for (MultipartFile file : files)
            sumOfImageSizes += file.getSize();
        if (sumOfImageSizes > 1 * 1024 * 1024) // 1MB
            return StatusCode.EXCEED_1MB.makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_MISSION_IMAGE, uuid);

        // save files
        StatusCode code = missionService.uploadMissionImages(files, id, uuid);
        if (code.checkIsError())
            return code.makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_MISSION_IMAGE, uuid);
        return StatusCode.NO_ERROR.makeErrorResponseEntityAndPrintLog(LogHeader.UPLOAD_MISSION_IMAGE, uuid);
    }

    // delete all images of #{id} mission.
    @DeleteMapping("/mission-image/{id}")
    public ResponseEntity<?> deleteAllMissionImage(Authentication authentication, @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode code = missionService.deleteAllMissionImages(id, uuid);
        return code.makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_MISSION_IMAGE, uuid);
    }
}
