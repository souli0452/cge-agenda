package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.ParticipantEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = { ScheduleMapper.class, ParticipantMapper.class, FileMapper.class })
public interface EventMapper {

    // ==========================================
    // Entity → DTO
    // ==========================================
    @Mapping(target = "globalStartTime", ignore = true)
    @Mapping(target = "globalEndTime", ignore = true)
    @Mapping(source = "schedules", target = "schedules")
    @Mapping(source = "participantEvents", target = "participants", qualifiedByName = "participantEventsToParticipants")
    @Mapping(source = "files", target = "files")
    EventDto toDto(Event event);

    // ==========================================
    // Méthode pour convertir Set<ParticipantEvent> → List<ParticipantDto>
    // ==========================================
    @Named("participantEventsToParticipants")
    default List<ParticipantDto> participantEventsToParticipants(Set<ParticipantEvent> participantEvents) {
        if (participantEvents == null || participantEvents.isEmpty()) {
            return List.of();
        }

        return participantEvents.stream()
                .map(pe -> ParticipantDto.builder()
                        .id(pe.getParticipant().getId())
                        .firstName(pe.getParticipant().getFirstName())
                        .lastName(pe.getParticipant().getLastName())
                        .email(pe.getParticipant().getEmail())
                        .phoneNumber(pe.getParticipant().getPhoneNumber())
                        .organization(pe.getParticipant().getOrganization())
                        .jobTitle(pe.getParticipant().getJobTitle())
                        .participantType(pe.getParticipant().getParticipantType())
                        .createdAt(pe.getParticipant().getCreatedAt())
                        .updatedAt(pe.getParticipant().getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ==========================================
    // DTO → Entity
    // ==========================================
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "participantEvents", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "updatedById", ignore = true)
    @Mapping(target = "currentFirstName", ignore = true)
    @Mapping(target = "currentLastName", ignore = true)
    @Mapping(target = "currentUserEmail", ignore = true)
    Event toEntity(EventDto eventDto);

    // ==========================================
    // Listes
    // ==========================================
    List<EventDto> toDtos(List<Event> events);

    List<Event> toEntities(List<EventDto> eventDtos);

    // ==========================================
    // Update
    // ==========================================
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "participantEvents", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "updatedById", ignore = true)
    @Mapping(target = "currentFirstName", ignore = true)
    @Mapping(target = "currentLastName", ignore = true)
    @Mapping(target = "currentUserEmail", ignore = true)
    void updateEntityFromDto(EventDto eventDto, @MappingTarget Event event);
}