package com.cbnuccc.cbnuccc.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.PrayerDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.Prayer;
import com.cbnuccc.cbnuccc.Repository.PrayerJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrayerService {
    private final UserJpaRepository userJpaRepository;
    private final PrayerJpaRepository prayerJpaRepository;

    // make Prayer to PrayerDto
    private PrayerDto prayerToPrayerDto(Prayer prayer) {
        UUID authorUuid = prayerJpaRepository.findAuthorUuidByPrayerId(prayer.getId()).get(); // get author's id
        return new PrayerDto(
                prayer.getId(),
                authorUuid,
                prayer.getCreatedAt(),
                prayer.getRequest(),
                prayer.getAnonymous());
    }

    // get all prayers except anonymous ones.
    public List<PrayerDto> getAllNotAnonymousPrayers() {
        // get all opened prayers.
        List<Prayer> prayers = prayerJpaRepository.findAllByAnonymousFalse();
        List<PrayerDto> result = new ArrayList<PrayerDto>();
        for (Prayer prayer : prayers)
            result.add(prayerToPrayerDto(prayer));
        return result;
    }

    // get a specific prayer except anonymous one.
    public DataWithStatusCode<PrayerDto> getNotAnonymousSpecificPrayer(int id) {
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAnonymousFalse(id);
        if (_prayer.isEmpty()) // not exist or it's anonymous
            return new DataWithStatusCode<>(StatusCode.NO_PRAYER_FOUND, null);

        PrayerDto result = prayerToPrayerDto(_prayer.get());
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, result);
    }

    // get all prayers of specific user.
    public List<PrayerDto> getAllPrayersByUuid(UUID uuid) {
        List<Prayer> prayers = prayerJpaRepository.findAllByAuthorUuid(uuid);
        List<PrayerDto> result = new ArrayList<PrayerDto>();
        for (Prayer prayer : prayers)
            result.add(prayerToPrayerDto(prayer));
        return result;
    }

    // get a specific prayer
    public DataWithStatusCode<PrayerDto> getPrayerById(int id, UUID uuid) {
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_prayer.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_PRAYER_FOUND, null);
        PrayerDto result = prayerToPrayerDto(_prayer.get());
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, result);
    }

    // create a prayer.
    public DataWithStatusCode<PrayerDto> createPrayer(PrayerDto prayerDto, UUID uuid) {
        // get author info.
        Optional<MyUser> _author = userJpaRepository.findByUuid(uuid);
        if (_author.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_USER_FOUND, null);
        MyUser author = _author.get();

        // set prayer data
        Prayer prayer = new Prayer();
        prayer.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        prayer.setRequest(prayerDto.getRequest());
        prayer.setAnonymous(prayerDto.getAnonymous());
        prayer.setAuthor(author);

        // save it
        try {
            Prayer craetedPrayer = prayerJpaRepository.save(prayer);
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, prayerToPrayerDto(craetedPrayer));
        } catch (Exception e) {
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // update a prayer
    public StatusCode updatePrayer(int id, UUID uuid, PrayerDto prayerDto) {
        // find author info.
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_prayer.isEmpty())
            return StatusCode.NO_PRAYER_FOUND;
        Prayer prayer = _prayer.get();

        // update by fiven data.
        prayer.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        if (prayerDto.getRequest() != null)
            prayer.setRequest(prayerDto.getRequest());
        if (prayerDto.getAnonymous() != null)
            prayer.setAnonymous(prayerDto.getAnonymous());

        // update it
        try {
            prayerJpaRepository.save(prayer);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_PRAYER, e.getMessage(), uuid);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // delete a prayer
    public StatusCode deletePrayer(int id, UUID uuid) {
        // find author info.
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_prayer.isEmpty())
            return StatusCode.NO_PRAYER_FOUND;
        Prayer prayer = _prayer.get();

        // delete it
        try {
            prayerJpaRepository.delete(prayer);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PRAYER, e.getMessage(), uuid);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }
}
