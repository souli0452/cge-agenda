package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.mapper.FileMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.FileRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FileMapper fileMapper;
}
