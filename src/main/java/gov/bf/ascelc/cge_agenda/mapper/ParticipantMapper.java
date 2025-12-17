package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.dto.ParticipantEventDto;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.ParticipantEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ParticipantMapper {

    ParticipantDto toDto(Participant participant);

    Participant toEntity(ParticipantDto participantDto);

    List<ParticipantDto> toDtos(List<Participant> participants);

    List<Participant> toEntities(List<ParticipantDto> participantDtos);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ParticipantDto participantDto, @MappingTarget Participant participant);
}
