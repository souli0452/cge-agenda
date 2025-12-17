package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.ScheduleDto;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {
    ScheduleDto create(ScheduleDto dto);
    List<ScheduleDto> findByEventId(UUID eventId);
    void delete(UUID id);
}
