package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileService {

    /**
     * Upload a file and attach it to an event
     */
    FileDto uploadFile(UUID eventId, MultipartFile file, String description);

    /**
     * Download a file
     */
    Resource downloadFile(UUID fileId);

    /**
     * Delete a file
     */
    void deleteFile(UUID fileId);

    /**
     * Get all files for an event
     */
    List<FileDto> getFilesByEvent(UUID eventId);

    /**
     * Get file details
     */
    FileDto getFileById(UUID fileId);
}