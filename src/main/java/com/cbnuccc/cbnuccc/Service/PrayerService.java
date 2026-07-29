package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.PrayerDto;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Model.Prayer;
import com.cbnuccc.cbnuccc.Repository.PrayerJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrayerService {
    private final UserJpaRepository userJpaRepository;
    private final PrayerJpaRepository prayerJpaRepository;

    // Prayer를 PrayerDto로 변환하기
    private PrayerDto prayerToPrayerDto(Prayer prayer) {
        UUID authorUuid = prayerJpaRepository.findAuthorUuidByPrayerId(prayer.getId()).get(); // 작성자의 id 가져오기
        return new PrayerDto(
                prayer.getId(),
                authorUuid,
                prayer.getCreatedAt(),
                prayer.getRequest(),
                prayer.getAnonymous());
    }

    // 익명이 아닌 모든 기도 가져오기
    public Page<PrayerDto> getAllNotAnonymousPrayers(Pageable pageable) {
        // 공개된 모든 기도 가져오기
        Page<Prayer> prayers = prayerJpaRepository.findAllByAnonymousFalse(pageable);
        return prayers.map(prayer -> prayerToPrayerDto(prayer));
    }

    // 익명이 아닌 특정 기도 가져오기
    public DataWithStatusCode<PrayerDto> getNotAnonymousSpecificPrayer(int id) {
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAnonymousFalse(id);
        if (_prayer.isEmpty()) // 존재하지 않거나 익명임
            return new DataWithStatusCode<>(StatusCode.NO_PRAYER_FOUND, null);

        PrayerDto result = prayerToPrayerDto(_prayer.get());
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, result);
    }

    // 특정 사용자의 모든 기도 가져오기
    public Page<PrayerDto> getAllPrayersByUuid(UUID uuid, Pageable pageable) {
        Page<Prayer> prayers = prayerJpaRepository.findAllByAuthorUuid(uuid, pageable);
        return prayers.map(prayer -> prayerToPrayerDto(prayer));
    }

    // 특정 기도 가져오기
    public DataWithStatusCode<PrayerDto> getPrayerById(int id, UUID uuid) {
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_prayer.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_PRAYER_FOUND, null);
        PrayerDto result = prayerToPrayerDto(_prayer.get());
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, result);
    }

    // 기도 생성하기
    public DataWithStatusCode<PrayerDto> createPrayer(PrayerDto prayerDto, UUID uuid) {
        // 작성자 정보 가져오기
        Optional<MyUser> _author = userJpaRepository.findByUuid(uuid);
        if (_author.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_USER_FOUND, null);
        MyUser author = _author.get();

        // 기도 데이터 설정하기
        Prayer prayer = new Prayer();
        prayer.setCreatedAt(OffsetDateTimeUtil.getNow());
        prayer.setRequest(prayerDto.getRequest());
        prayer.setAnonymous(prayerDto.getAnonymous());
        prayer.setAuthor(author);

        // 저장하기
        try {
            Prayer craetedPrayer = prayerJpaRepository.save(prayer);
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, prayerToPrayerDto(craetedPrayer));
        } catch (Exception e) {
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // 기도 수정하기
    public StatusCode updatePrayer(int id, UUID uuid, PrayerDto prayerDto) {
        // 작성자 정보 찾기
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_prayer.isEmpty())
            return StatusCode.NO_PRAYER_FOUND;
        Prayer prayer = _prayer.get();

        // 주어진 데이터로 수정하기
        prayer.setCreatedAt(OffsetDateTimeUtil.getNow());
        if (prayerDto.getRequest() != null)
            prayer.setRequest(prayerDto.getRequest());
        if (prayerDto.getAnonymous() != null)
            prayer.setAnonymous(prayerDto.getAnonymous());

        // 수정하기
        try {
            prayerJpaRepository.save(prayer);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_PRAYER, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // 기도 삭제하기
    public StatusCode deletePrayer(int id, UUID uuid) {
        // 작성자 정보 찾기
        Optional<Prayer> _prayer = prayerJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_prayer.isEmpty())
            return StatusCode.NO_PRAYER_FOUND;
        Prayer prayer = _prayer.get();

        // 삭제하기
        try {
            prayerJpaRepository.delete(prayer);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PRAYER, LogUtil.makeExceptionKV(e));
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // 모든 기도 작성자의 uuid 가져오기
    public Page<UUID> getAllAuthorUuid(Pageable pageable) {
        return prayerJpaRepository.findAuthorUuid(pageable);
    }
}
