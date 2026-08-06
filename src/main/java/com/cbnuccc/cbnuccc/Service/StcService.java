package com.cbnuccc.cbnuccc.Service;

import java.util.Optional;
import java.util.UUID;

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
}
