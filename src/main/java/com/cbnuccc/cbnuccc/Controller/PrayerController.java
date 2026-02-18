package com.cbnuccc.cbnuccc.Controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.cbnuccc.cbnuccc.Dto.PrayerDto;
import com.cbnuccc.cbnuccc.Service.PrayerService;
import com.cbnuccc.cbnuccc.Service.UserService;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PrayerController {
    private final UserService userService;
    private final PrayerService prayerService;

    // get all prayers but not anonymous ones.
    @GetMapping("/prayer")
    public ResponseEntity<?> getPrayers() {
        List<PrayerDto> result = prayerService.getAllNotAnonymousPrayers();

        // print log
        String message = String.format("successfully got %d all user's prayers.", result.size());
        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, message, null);

        return ResponseEntity.ok(result);
    }

    // get a specific prayer.
    @GetMapping("/prayer/{id}")
    public ResponseEntity<?> getPrayersById(@PathVariable("id") int id) {
        DataWithStatusCode<PrayerDto> result = prayerService.getNotAnonymousSpecificPrayer(id);
        if (result.code().checkIsError())
            return result.code().makeErrorResponseEntityAndPrintLog(LogHeader.GET_PRAYER, null);

        // print log
        String message = String.format("successfully got specific prayer #%d.", id);
        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, message, null);

        return ResponseEntity.ok(result.data());
    }

    // get all my prayers.
    @GetMapping("/my-prayer")
    public ResponseEntity<?> getMyPrayers(Authentication authentication) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        List<PrayerDto> result = prayerService.getAllPrayersByUuid(uuid);

        // print log
        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, "successfully got my prayers.", uuid);

        return ResponseEntity.ok(result);
    }

    // get all my prayers.
    @GetMapping("/my-prayer/{id}")
    public ResponseEntity<?> getMyPrayerById(Authentication authentication, @PathVariable("id") int id) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<PrayerDto> result = prayerService.getPrayerById(id, uuid);
        if (result.code().checkIsError()) // check error
            return result.code().makeErrorResponseEntityAndPrintLog(LogHeader.GET_PRAYER, uuid);

        // print log
        String message = String.format("successfully got my specific prayer #%d.", id);
        LogUtil.printBasicInfoLog(LogHeader.GET_PRAYER, message, uuid);

        return ResponseEntity.ok(result.data());
    }

    // create a prayer.
    @PostMapping("/prayer")
    public ResponseEntity<?> createPrayer(Authentication authentication, @RequestBody PrayerDto prayerDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        DataWithStatusCode<PrayerDto> result = prayerService.createPrayer(prayerDto, uuid);
        if (result.code().checkIsError())
            result.code().makeErrorResponseEntityAndPrintLog(LogHeader.CREATE_PRAYER, uuid);

        // print log
        LogUtil.printBasicInfoLog(LogHeader.CREATE_PRAYER, "successfully create a prayer", uuid);

        return ResponseEntity.ok(result.data());
    }

    // update a prayer
    @PatchMapping("/prayer/{id}")
    public ResponseEntity<?> updatePrayer(
            Authentication authentication,
            @PathVariable("id") int id,
            @RequestBody PrayerDto prayerDto) {
        UUID uuid = userService.getUuidFromAuth(authentication);
        StatusCode result = prayerService.updatePrayer(id, uuid, prayerDto);
        if (result.checkIsError())
            return result.makeErrorResponseEntityAndPrintLog(LogHeader.UPDATE_PRAYER, uuid);

        // print log
        String message = String.format("successfully update a prayer #%d", id);
        LogUtil.printBasicInfoLog(LogHeader.UPDATE_PRAYER, message, uuid);

        return getMyPrayerById(authentication, id);
    }

    // delete a prayer
    @DeleteMapping("/prayer/{id}")
    public ResponseEntity<?> deletePrayer(
            Authentication authentication,
            @PathVariable("id") int id) {
        // get auth info.
        UUID uuid = userService.getUuidFromAuth(authentication);

        // get being deleted data
        DataWithStatusCode<PrayerDto> _deletedPrayer = prayerService.getPrayerById(id, uuid);
        if (_deletedPrayer.code().checkIsError())
            return _deletedPrayer.code().makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_PRAYER, uuid);
        PrayerDto deletedPrayer = _deletedPrayer.data();

        // delete data
        StatusCode result = prayerService.deletePrayer(id, uuid);
        if (result.checkIsError())
            return result.makeErrorResponseEntityAndPrintLog(LogHeader.DELETE_PRAYER, uuid);

        // print log
        String message = String.format("successfully delete a prayer #%d", id);
        LogUtil.printBasicInfoLog(LogHeader.DELETE_PRAYER, message, uuid);

        return ResponseEntity.ok(deletedPrayer);
    }
}
