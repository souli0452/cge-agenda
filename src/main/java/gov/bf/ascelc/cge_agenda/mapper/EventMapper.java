package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;


@Mapper(componentModel = "spring")
public interface EventMapper {

    EventDto toDto(Event event);

    Event toEntity(EventDto eventDto);

    List<EventDto> toDtos(List<Event> events);

    List<Event> toEntities(List<EventDto> eventDtos);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(EventDto eventDto, @MappingTarget Event event);
}
