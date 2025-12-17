package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.dto.ScheduleDto;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    ScheduleDto toDto(Schedule schedule);

    Schedule toEntity(ScheduleDto scheduleDto);

    List<ScheduleDto> toDtos(List<Schedule> schedules);

    List<Schedule> toEntities(List<ScheduleDto> scheduleDtos);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ScheduleDto scheduleDto, @MappingTarget Schedule schedule);
}
