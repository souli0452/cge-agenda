package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import gov.bf.ascelc.cge_agenda.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(FILE_ROOT_URL)
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = UPLOAD_FILE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> uploadFile(
            @RequestParam("eventId") UUID eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description
    ) {
        FileDto uploadedFile = fileService.uploadFile(eventId, file, description);
        return new ResponseEntity<>(uploadedFile, HttpStatus.CREATED);
    }

    @GetMapping(DOWNLOAD_FILE)
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        FileDto fileDto = fileService.getFileById(id);
        Resource resource = fileService.downloadFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        fileDto.getFileType() != null
                                ? fileDto.getFileType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileDto.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping(DELETE_FILE)
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(GET_FILES_BY_EVENT)
    public ResponseEntity<List<FileDto>> getFilesByEvent(@PathVariable UUID eventId) {
        List<FileDto> files = fileService.getFilesByEvent(eventId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileDto> getFileById(@PathVariable UUID id) {
        FileDto file = fileService.getFileById(id);
        return ResponseEntity.ok(file);
    }
}