package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.mapper.FileMapper;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantEventMapper;
import gov.bf.ascelc.cge_agenda.repository.FileRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.ParticipantEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantEventServiceImpl implements ParticipantEventService {

    private final ParticipantEventRepository participantEventRepository;
    private final ParticipantEventMapper participantEventMapper;
}
