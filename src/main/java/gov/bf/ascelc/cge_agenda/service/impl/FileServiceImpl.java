package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.mapper.FileMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.FileRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    @Override
    public FileDto uploadFile(MultipartFile file, UUID eventId) {
        return null;
    }

    @Override
    public FileDto findById(UUID id) {
        return null;
    }

    @Override
    public List<FileDto> findByEventId(UUID eventId) {
        return List.of();
    }

    @Override
    public void delete(UUID id) {

    }

    @Override
    public byte[] downloadFile(UUID id) {
        return new byte[0];
    }
}
