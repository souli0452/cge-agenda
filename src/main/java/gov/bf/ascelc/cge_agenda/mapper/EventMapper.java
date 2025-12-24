package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

@Mapper(componentModel = "spring", uses = {ScheduleMapper.class})
public interface EventMapper {

    // ==========================================
    // Entity → DTO
    // ==========================================
    @Mapping(target = "globalStartTime", ignore = true)
    @Mapping(target = "globalEndTime", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(source = "schedules", target = "schedules")
    EventDto toDto(Event event);

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