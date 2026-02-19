package com.cbnuccc.cbnuccc.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cbnuccc.cbnuccc.Dto.MissionDto;
import com.cbnuccc.cbnuccc.Model.Mission;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Repository.MissionJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionService {
    private final MissionJpaRepository missionJpaRepository;
    private final UserJpaRepository userJpaRepository;

    private MissionDto missionToMissionDto(Mission mission) {
        return new MissionDto(
                mission.getId(),
                missionJpaRepository.findAuthorUuidByMissionId(mission.getId()).get(),
                mission.getCreatedAt(),
                mission.getSite(),
                mission.getStartTerm(),
                mission.getEndTerm(),
                mission.getSeason());
    }

    // get all missions
    public List<MissionDto> getAllMissions() {
        List<Mission> missions = missionJpaRepository.findAll();
        List<MissionDto> result = new ArrayList<>();
        for (Mission mission : missions)
            result.add(missionToMissionDto(mission));
        return result;
    }

    // get a specific mission.
    public DataWithStatusCode<MissionDto> getSpecificMission(int id) {
        Optional<Mission> _mission = missionJpaRepository.findById(id);
        if (_mission.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_MISSION_FOUND, null);
        Mission mission = _mission.get();
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, missionToMissionDto(mission));
    }

    // get all my missions
    public List<MissionDto> getAllMyMissions(UUID uuid) {
        List<Mission> missions = missionJpaRepository.findAllByAuthorUuid(uuid);
        List<MissionDto> result = new ArrayList<>();
        for (Mission mission : missions)
            result.add(missionToMissionDto(mission));
        return result;
    }

    // create a mission
    public DataWithStatusCode<MissionDto> createMission(MissionDto missionDto, UUID uuid) {
        // find author user.
        Optional<MyUser> _user = userJpaRepository.findByUuid(uuid);
        if (_user.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_USER_FOUND, null);
        MyUser user = _user.get();

        // create a mission instance
        Mission mission = new Mission();
        mission.setAuthor(user);
        mission.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        mission.setSite(missionDto.getSite());
        mission.setStartTerm(missionDto.getStartTerm());
        mission.setEndTerm(missionDto.getEndTerm());
        mission.setSeason(missionDto.getSeason());

        try {
            // save it.
            Mission createdMission = missionJpaRepository.save(mission);
            return new DataWithStatusCode<>(StatusCode.NO_ERROR, missionToMissionDto(createdMission));
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.CREATE_MISSION, e.getMessage(), uuid);
            return new DataWithStatusCode<>(StatusCode.SOMETHING_WENT_WRONG, null);
        }
    }

    // update a mission.
    public StatusCode updateMission(int id, UUID uuid, MissionDto missionDto) {
        // check existance
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // update mission.
        missionDto.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        if (missionDto.getSite() != null)
            mission.setSite(missionDto.getSite());
        if (missionDto.getStartTerm() != null)
            mission.setStartTerm(missionDto.getStartTerm());
        if (missionDto.getEndTerm() != null)
            mission.setEndTerm(missionDto.getEndTerm());
        if (missionDto.getSeason() != null)
            mission.setSeason(missionDto.getSeason());

        try {
            missionJpaRepository.save(mission);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_MISSION, e.getMessage(), uuid);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // delete a mission.
    public StatusCode deleteMission(int id, UUID uuid) {
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;

        try {
            missionJpaRepository.deleteById(id);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION, e.getMessage(), null);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }
}
