package gov.bf.ascelc.cge_agenda.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class FileStorageConfig {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Crée automatiquement les dossiers au démarrage de l'application
     */
    @Bean
    public CommandLineRunner initStorage() {
        return args -> {
            try {
                // Créer uploads/
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Dossier créé : {}", uploadPath.toAbsolutePath());
                } else {
                    log.info("✓ Dossier existant : {}", uploadPath.toAbsolutePath());
                }

                // Créer uploads/events/
                Path eventsPath = uploadPath.resolve("events");
                if (!Files.exists(eventsPath)) {
                    Files.createDirectories(eventsPath);
                    log.info("Dossier créé : {}", eventsPath.toAbsolutePath());
                } else {
                    log.info("Dossier existant : {}", eventsPath.toAbsolutePath());
                }

                log.info("Système de stockage initialisé avec succès !");
                log.info("Dossier d'upload : {}", uploadPath.toAbsolutePath());

            } catch (Exception e) {
                log.error("ERREUR lors de l'initialisation du stockage : {}", e.getMessage());
                throw new RuntimeException("Impossible de créer les dossiers de stockage", e);
            }
        };
    }
}