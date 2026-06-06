package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.File;
import gov.bf.ascelc.cge_agenda.mapper.FileMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.FileRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final EventRepository eventRepository;
    private final FileMapper fileMapper;
    private final EmailService emailService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "txt", "zip", "rar"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public FileDto uploadFile(UUID eventId, MultipartFile file, String description) {
        log.info("🔄 Upload de fichier pour l'événement : {}", eventId);
        validateUploadRequest(eventId, file);

        try {
            Path eventPath = createEventDirectory(eventId);
            String storedFilename = generateSecureFilename(file.getOriginalFilename());
            Path targetLocation = eventPath.resolve(storedFilename);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ Fichier enregistré : {}", targetLocation.toAbsolutePath());

            File fileEntity = buildFileEntity(eventId, file, description, targetLocation.toString());
            fileEntity = fileRepository.save(fileEntity);

            sendEmailNotification(eventId, fileEntity);
            return fileMapper.toDto(fileEntity);

        } catch (IOException e) {
            log.error("Erreur I/O lors de l'upload : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de l'enregistrement du fichier"
            );
        }
    }

    private void validateUploadRequest(UUID eventId, MultipartFile file) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide ou manquant");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux (max 10 Mo)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
        }


        String extension = extractExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Type de fichier non autorisé : " + extension
            );
        }

        if (originalFilename.contains("..") || originalFilename.contains("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caractères illégaux dans le nom de fichier");
        }
    }



    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1)
                : "";
    }

    // ==========================================
    // GESTION DES DOSSIERS
    // ==========================================

    private Path createEventDirectory(UUID eventId) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable"));

        Path uploadPath = Paths.get(uploadDir, "events");
        Files.createDirectories(uploadPath);

        String folderName = createEventFolderName(event);
        Path eventPath = uploadPath.resolve(folderName);
        Files.createDirectories(eventPath);

        log.debug("Dossier événement créé : {}", eventPath);
        return eventPath;
    }

    private String createEventFolderName(Event event) {
        String shortUuid = event.getId().toString().substring(0, 8);
        String normalizedTitle = normalizeString(event.getTitle());
        String cleanTitle = normalizedTitle
                .replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (cleanTitle.length() > 50) {
            cleanTitle = cleanTitle.substring(0, 50);
        }

        return shortUuid + "-" + cleanTitle;
    }

    // ==========================================
    // GESTION DES FICHIERS
    // ==========================================

    private String generateSecureFilename(String originalFilename) {
        String extension = extractExtension(originalFilename);
        String nameWithoutExt = (extension.isEmpty())
                ? originalFilename
                : originalFilename.substring(0, originalFilename.lastIndexOf('.'));

        String cleanName = normalizeString(nameWithoutExt)
                .replaceAll("[^a-zA-Z0-9_-]", "_");

        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return randomSuffix + "_" + cleanName + (extension.isEmpty() ? "" : "." + extension);
    }

    private String normalizeString(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private File buildFileEntity(UUID eventId, MultipartFile file, String description, String filePath) {
        return File.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .description(description != null ? description : "")
                .event(eventRepository.getReferenceById(eventId))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void sendEmailNotification(UUID eventId, File fileEntity) {
        try {
            emailService.sendNewDocumentNotification(eventId, fileEntity.getId());
            log.info("Notification email envoyée pour le fichier : {}", fileEntity.getFileName());
        } catch (Exception e) {
            log.warn("Échec de la notification email : {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(UUID fileId) {
        log.info("Téléchargement du fichier : {}", fileId);
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable"));

        try {
            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("Fichier téléchargé : {}", file.getFileName());
                return resource;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable sur le disque");
        } catch (MalformedURLException e) {
            log.error("URL invalide pour le fichier : {}", file.getFilePath());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de téléchargement");
        }
    }

    @Override
    public void deleteFile(UUID fileId) {
        log.info(" Suppression du fichier : {}", fileId);
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable"));

        try {
            Path filePath = Paths.get(file.getFilePath());
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Fichier physique supprimé : {}", file.getFileName());
            }

            fileRepository.delete(file);
            log.info("Fichier supprimé de la base : {}", file.getFileName());
        } catch (IOException e) {
            log.error("❌ Erreur lors de la suppression physique : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Échec de la suppression");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDto> getFilesByEvent(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable");
        }

        List<File> files = fileRepository.findByEventId(eventId);
        log.info("{} fichiers trouvés pour l'événement {}", files.size(), eventId);
        return fileMapper.toDtos(files);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDto getFileById(UUID fileId) {
        return fileRepository.findById(fileId)
                .map(file -> {
                    log.info("🔍 Fichier trouvé : {}", file.getFileName());
                    return fileMapper.toDto(file);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable"));
    }
}