package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import gov.bf.ascelc.cge_agenda.dto.ParticipantEventDto;
import gov.bf.ascelc.cge_agenda.entities.File;
import gov.bf.ascelc.cge_agenda.entities.ParticipantEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ParticipantEventMapper {
    ParticipantEventDto toDto(ParticipantEvent participantEvent);

    ParticipantEvent toEntity(ParticipantEventDto participantEventDto);

    List<ParticipantEventDto> toDtos(List<ParticipantEvent> participantEvents);

    List<ParticipantEvent> toEntities(List<ParticipantEventDto> participantEventDtos);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ParticipantEventDto participantEventDto, @MappingTarget ParticipantEvent participantEvent);
}
