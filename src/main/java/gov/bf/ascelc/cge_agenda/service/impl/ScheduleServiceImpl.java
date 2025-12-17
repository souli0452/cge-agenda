package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.ScheduleDto;
import gov.bf.ascelc.cge_agenda.mapper.ScheduleMapper;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    public ScheduleDto create(ScheduleDto dto) {
        return null;
    }

    @Override
    public List<ScheduleDto> findByEventId(UUID eventId) {
        return List.of();
    }

    @Override
    public void delete(UUID id) {

    }
}
