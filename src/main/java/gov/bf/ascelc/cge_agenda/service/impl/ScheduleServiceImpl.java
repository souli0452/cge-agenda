package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.mapper.ParticipantEventMapper;
import gov.bf.ascelc.cge_agenda.mapper.ScheduleMapper;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
}
