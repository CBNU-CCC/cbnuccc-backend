package com.cbnuccc.cbnuccc.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.cbnuccc.cbnuccc.Config.SupabaseProperties;
import com.cbnuccc.cbnuccc.Dto.MissionDto;
import com.cbnuccc.cbnuccc.Model.Mission;
import com.cbnuccc.cbnuccc.Model.MyUser;
import com.cbnuccc.cbnuccc.Repository.MissionJpaRepository;
import com.cbnuccc.cbnuccc.Repository.UserJpaRepository;
import com.cbnuccc.cbnuccc.Util.DataWithStatusCode;
import com.cbnuccc.cbnuccc.Util.LogHeader;
import com.cbnuccc.cbnuccc.Util.LogUtil;
import com.cbnuccc.cbnuccc.Util.OffsetDateTimeUtil;
import com.cbnuccc.cbnuccc.Util.StatusCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionService {
    private final MissionJpaRepository missionJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final WebClient webClient;
    private final SupabaseProperties supabaseProperties;

    private MissionDto missionToMissionDto(Mission mission) {
        return new MissionDto(
                mission.getId(),
                missionJpaRepository.findAuthorUuidByMissionId(mission.getId()).get(),
                mission.getCreatedAt(),
                mission.getSite(),
                mission.getStartTerm(),
                mission.getEndTerm(),
                mission.getSeason(),
                mission.getTestimony(),
                mission.getImageCount());
    }

    private void deleteSpecificMissionImage(long id, short imageId) {
        // set file name
        String fileName = String.format("%d-%d", id, imageId);
        String path = "mission/" + fileName;

        // delete
        webClient.delete()
                .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                .header("Authorization", "Bearer " + supabaseProperties.getKey())
                .header("apikey", supabaseProperties.getKey())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // get all missions
    public Page<MissionDto> getAllMissions(Pageable pageable) {
        Page<Mission> missions = missionJpaRepository.findAll(pageable);
        return missions.map(mission -> missionToMissionDto(mission));
    }

    // get a specific mission.
    public DataWithStatusCode<MissionDto> getSpecificMission(long id) {
        Optional<Mission> _mission = missionJpaRepository.findById(id);
        if (_mission.isEmpty())
            return new DataWithStatusCode<>(StatusCode.NO_MISSION_FOUND, null);
        Mission mission = _mission.get();
        return new DataWithStatusCode<>(StatusCode.NO_ERROR, missionToMissionDto(mission));
    }

    // get all my missions
    public Page<MissionDto> getAllMyMissions(UUID uuid, Pageable pageable) {
        Page<Mission> missions = missionJpaRepository.findAllByAuthorUuid(uuid, pageable);
        return missions.map(mission -> missionToMissionDto(mission));
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
        mission.setCreatedAt(OffsetDateTimeUtil.getNow());
        mission.setSite(missionDto.getSite());
        mission.setStartTerm(missionDto.getStartTerm());
        mission.setEndTerm(missionDto.getEndTerm());
        mission.setSeason(missionDto.getSeason());
        mission.setTestimony(missionDto.getTestimony());
        mission.setImageCount((short) 0);

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
    public StatusCode updateMission(long id, UUID uuid, MissionDto missionDto) {
        // check existance
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // update mission.
        missionDto.setCreatedAt(OffsetDateTimeUtil.getNow());
        if (missionDto.getSite() != null)
            mission.setSite(missionDto.getSite());
        if (missionDto.getStartTerm() != null)
            mission.setStartTerm(missionDto.getStartTerm());
        if (missionDto.getEndTerm() != null)
            mission.setEndTerm(missionDto.getEndTerm());
        if (missionDto.getSeason() != null)
            mission.setSeason(missionDto.getSeason());
        if (missionDto.getTestimony() != null)
            mission.setTestimony(missionDto.getTestimony());

        try {
            missionJpaRepository.save(mission);
            return StatusCode.NO_ERROR;
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPDATE_MISSION, e.getMessage(), uuid);
            return StatusCode.SOMETHING_WENT_WRONG;
        }
    }

    // delete a mission.
    public StatusCode deleteMission(long id, UUID uuid) {
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

    // get all mission author's uuid
    public Page<UUID> getAllAuthorUuid(Pageable pageable) {
        return missionJpaRepository.findAuthorUuid(pageable);
    }

    // upload mission images.
    public StatusCode uploadMissionImages(List<MultipartFile> files, long id, UUID uuid) {
        // verify auth information to update #{id} mission board.
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // set mission's image count to 0
        mission.setImageCount((short) 0);

        for (short i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            try {
                // extract extension
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains("."))
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));

                // set file name
                String fileName = String.format("%d-%d", id, i);
                String path = "mission/" + fileName;

                // upload it
                webClient.post()
                        .uri(supabaseProperties.getUrl() + "/storage/v1/object/" + path)
                        .header("Authorization", "Bearer " + supabaseProperties.getKey())
                        .header("apikey", supabaseProperties.getKey())
                        .header("Content-Type", "image/" + extension)
                        .header("x-upsert", "true")
                        .contentType(MediaType.parseMediaType(file.getContentType()))
                        .bodyValue(file.getBytes())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (Exception e) {
                deleteAllMissionImages(id, uuid);
                LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, e.getMessage(), uuid);
                return StatusCode.SOMETHING_WENT_WRONG;
            }
        }

        mission.setImageCount((short) files.size());
        try {
            missionJpaRepository.save(mission);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.UPLOAD_PROFILE_IMAGE, e.getMessage(), uuid);
        }
        return StatusCode.NO_ERROR;
    }

    public StatusCode deleteAllMissionImages(long id, UUID uuid) {
        // check if #{id} mission is made by user whose uuid is {uuid}
        Optional<Mission> _mission = missionJpaRepository.findByIdAndAuthorUuid(id, uuid);
        if (_mission.isEmpty())
            return StatusCode.NO_MISSION_FOUND;
        Mission mission = _mission.get();

        // delete all images
        short imageCount = mission.getImageCount();
        for (short i = 0; i < imageCount; i++) {
            try {
                // delete it
                deleteSpecificMissionImage(id, i);
            } catch (Exception e) {
                LogUtil.printBasicWarnLog(LogHeader.DELETE_MISSION_IMAGE, e.getMessage(), null);
                return StatusCode.SOMETHING_WENT_WRONG;
            }
        }

        // set image_count to 0, because there is no image in the storage.
        mission.setImageCount((short) 0);
        try {
            missionJpaRepository.save(mission);
        } catch (Exception e) {
            LogUtil.printBasicWarnLog(LogHeader.DELETE_PROFILE_IMAGE, e.getMessage(), uuid);
        }
        return StatusCode.NO_ERROR;
    }
}
