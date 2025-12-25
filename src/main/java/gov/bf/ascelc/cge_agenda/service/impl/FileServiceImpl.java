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
    private final EmailService emailService;

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
            // Créer uploads/
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Dossier racine créé : {}", uploadPath.toAbsolutePath());
            }

            //Créer uploads/events/
            Path eventsPath = uploadPath.resolve("events");
            if (!Files.exists(eventsPath)) {
                Files.createDirectories(eventsPath);
                log.info("Dossier events créé : {}", eventsPath.toAbsolutePath());
            }

            //Créer le nom de dossier : uuid-titre (avec normalisation des accents)
            String eventFolderName = createEventFolderName(event);
            Path eventPath = eventsPath.resolve(eventFolderName);

            if (!Files.exists(eventPath)) {
                Files.createDirectories(eventPath);
                log.info("Dossier événement créé : {}", eventFolderName);
            } else {
                log.info("Dossier événement existant : {}", eventFolderName);
            }

            //Préparer le nom du fichier
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            // Extraire l'extension
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex);
            }

            // Nettoyer le nom (sans extension)
            String nameWithoutExt = dotIndex > 0 ?
                    originalFilename.substring(0, dotIndex) : originalFilename;

            // Normaliser le nom du fichier aussi (enlever les accents)
            String normalizedName = Normalizer.normalize(nameWithoutExt, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");

            String cleanName = normalizedName.replaceAll("[^a-zA-Z0-9_-]", "_");

            // Générer UUID pour le fichier
            UUID fileId = UUID.randomUUID();
            String storedFilename = fileId.toString().substring(0, 8) + "_" + cleanName + extension;

            Path targetLocation = eventPath.resolve(storedFilename);

            //Copier le fichier
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier enregistré : {}", targetLocation.toAbsolutePath());

            //Créer l'entité File
            File fileEntity = File.builder()
                    .id(fileId)
                    .fileName(originalFilename)  // Nom original pour l'affichage
                    .filePath(targetLocation.toString())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .description(description)
                    .event(event)
                    .createdAt(LocalDateTime.now())
                    .build();

            fileEntity = fileRepository.save(fileEntity);
            log.info("Fichier uploadé : {} → {}", originalFilename, storedFilename);
            log.info("Chemin complet : {}", targetLocation.toAbsolutePath());

            //Envoyer notification email
            try {
                emailService.sendNewDocumentNotification(event, fileEntity);
                log.info("Notification email envoyée");
            } catch (Exception e) {
                log.error("Erreur notification email : {}", e.getMessage());
            }

            return fileMapper.toDto(fileEntity);

        } catch (IOException e) {
            log.error("Erreur lors de l'upload : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not upload file: " + e.getMessage()
            );
        }
    }

    /**
     * SOLUTION 1 : Crée un nom de dossier hybride avec NORMALISATION des accents
     *
     * Exemples de transformation :
     * - "Réunion Budget 2025" → "7d59c9dd-Reunion_Budget_2025"
     * - "Séminaire été" → "3f8e2a1b-Seminaire_ete"
     * - "Conférence française" → "8c9d0e1f-Conference_francaise"
     *
     * @param event L'événement pour lequel créer le nom de dossier
     * @return Nom du dossier au format : {uuid-8chars}-{titre-normalise}
     */
    private String createEventFolderName(Event event) {
        // 8 premiers caractères de l'UUID
        String shortUuid = event.getId().toString().substring(0, 8);

        // Normaliser les accents
        // NFD = Decompose (é devient e + accent combiné)
        // \\p{M} = Tous les accents combinés
        // Résultat : é → e, à → a, ç → c, etc.
        String normalizedTitle = Normalizer.normalize(event.getTitle(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        log.debug("Normalisation : '{}' → '{}'", event.getTitle(), normalizedTitle);

        // Nettoyer (garder seulement lettres, chiffres, _ et -)
        String cleanTitle = normalizedTitle
                .replaceAll("[^a-zA-Z0-9_-]", "_")  // Remplacer caractères spéciaux par _
                .replaceAll("_+", "_")               // Éviter double underscore
                .replaceAll("^_|_$", "");            // Enlever _ au début/fin

        // Limiter à 50 caractères
        if (cleanTitle.length() > 50) {
            cleanTitle = cleanTitle.substring(0, 50);
        }

        String folderName = shortUuid + "-" + cleanTitle;
        log.debug("Nom de dossier généré : {} (original: {})", folderName, event.getTitle());

        return folderName;
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
                log.info("Fichier téléchargé : {}", file.getFileName());
                return resource;
            } else {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found or not readable: " + file.getFileName()
                );
            }
        } catch (MalformedURLException e) {
            log.error("Erreur téléchargement : {}", e.getMessage());
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
            log.info("Fichier physique supprimé");

            // Supprimer l'entrée en base
            fileRepository.delete(file);
            log.info("Fichier supprimé de la base : {}", file.getFileName());

        } catch (IOException e) {
            log.error("Erreur suppression : {}", e.getMessage());
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