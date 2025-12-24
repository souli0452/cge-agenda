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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final EventRepository eventRepository;
    private final FileMapper fileMapper;
    private final EmailService emailService;  // ✅ AJOUTÉ

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public FileDto uploadFile(UUID eventId, MultipartFile file, String description) {
        log.info("Upload de fichier pour l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found with ID: " + eventId
                ));

        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot upload empty file"
            );
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path eventPath = uploadPath.resolve(eventId.toString());
            if (!Files.exists(eventPath)) {
                Files.createDirectories(eventPath);
            }

            // Nettoyer le nom du fichier (enlever caractères spéciaux)
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String cleanFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

            // Gérer les doublons : ajouter un suffixe si le fichier existe déjà
            Path targetLocation = eventPath.resolve(cleanFilename);
            String storedFilename = cleanFilename;

            int counter = 1;
            while (Files.exists(targetLocation)) {
                String nameWithoutExt = cleanFilename.substring(0, cleanFilename.lastIndexOf('.'));
                String extension = cleanFilename.substring(cleanFilename.lastIndexOf('.'));
                storedFilename = nameWithoutExt + "_" + counter + extension;
                targetLocation = eventPath.resolve(storedFilename);
                counter++;
            }

            // Copier le fichier
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            File fileEntity = File.builder()
                    .fileName(originalFilename)  // Nom original pour affichage
                    .filePath(targetLocation.toString())  // Chemin complet avec nom nettoyé
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .description(description)
                    .event(event)
                    .createdAt(LocalDateTime.now())
                    .build();

            fileEntity = fileRepository.save(fileEntity);
            log.info("Fichier uploadé : {} → {}", originalFilename, storedFilename);

            // ✅ ENVOYER LA NOTIFICATION EMAIL
            try {
                emailService.sendNewDocumentNotification(event, fileEntity);
                log.info("Notification email envoyée pour le fichier : {}", originalFilename);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de la notification email : {}", e.getMessage());
                // Ne pas bloquer l'upload si l'email échoue
            }

            return fileMapper.toDto(fileEntity);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not upload file: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(UUID fileId) {
        log.info("Téléchargement du fichier : {}", fileId);

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found with ID: " + fileId
                ));

        try {
            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("Fichier trouvé et lisible : {}", file.getFileName());
                return resource;
            } else {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found or not readable: " + file.getFileName()
                );
            }
        } catch (MalformedURLException e) {
            log.error("Erreur lors du téléchargement : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error downloading file: " + e.getMessage()
            );
        }
    }

    @Override
    public void deleteFile(UUID fileId) {
        log.info("Suppression du fichier : {}", fileId);

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found with ID: " + fileId
                ));

        try {
            // Supprimer le fichier physique
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);

            // Supprimer l'entrée en base
            fileRepository.delete(file);
            log.info("Fichier supprimé avec succès : {}", file.getFileName());

        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error deleting file: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDto> getFilesByEvent(UUID eventId) {
        log.info("Récupération des fichiers de l'événement : {}", eventId);

        // Vérifier que l'événement existe
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event not found with ID: " + eventId
            );
        }

        List<File> files = fileRepository.findByEventId(eventId);
        log.info("{} fichiers trouvés", files.size());

        return fileMapper.toDtos(files);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDto getFileById(UUID fileId) {
        log.info("Récupération du fichier : {}", fileId);

        return fileRepository.findById(fileId)
                .map(file -> {
                    log.info("Fichier trouvé : {}", file.getFileName());
                    return fileMapper.toDto(file);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found with ID: " + fileId
                ));
    }
}