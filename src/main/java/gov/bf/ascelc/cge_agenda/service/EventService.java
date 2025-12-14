package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.EventDto;

import java.util.List;
import java.util.UUID;

public interface EventService {
    EventDto create(EventDto dto);

    EventDto update(EventDto eventDto);

    List<EventDto> allEvents();

    EventDto getEventById(UUID id);

    void delete(UUID id);
}
