package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.mapper.ParticipantEventMapper;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantMapper;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ParticipantMapper participantMapper;
}
