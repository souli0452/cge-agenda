package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileService {
    FileDto uploadFile(MultipartFile file, UUID eventId);
    FileDto findById(UUID id);
    List<FileDto> findByEventId(UUID eventId);
    void delete(UUID id);
    byte[] downloadFile(UUID id);
}
