package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.dto.BackupInfoDto;
import gov.bf.ascelc.cge_agenda.entities.BackupConfig;
import gov.bf.ascelc.cge_agenda.repository.BackupConfigRepository;
import gov.bf.ascelc.cge_agenda.service.BackupService;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Surveillance technique quotidienne — remplace le silence total en cas de problème
 * (disque plein, sauvegarde automatique qui s'arrête sans que personne ne le remarque)
 * par une alerte email aux comptes ADMIN. Pas de vérification "tout va bien" envoyée :
 * uniquement des alertes, pour éviter la lassitude (alert fatigue).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthMonitorScheduler {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupService backupService;
    private final EmailService emailService;

    @Value("${app.monitoring.min-free-space-mb:2048}")
    private long minFreeSpaceMb;

    @Scheduled(cron = "0 0 6 * * *")
    public void checkSystemHealth() {
        checkDiskSpace();
        checkLastBackup();
    }

    private void checkDiskSpace() {
        try {
            File currentDisk = new File(".").getAbsoluteFile();
            long usableMb = currentDisk.getUsableSpace() / (1024 * 1024);
            if (usableMb > 0 && usableMb < minFreeSpaceMb) {
                log.warn("⚠ Espace disque bas : {} Mo restants", usableMb);
                emailService.sendSystemAlert(
                        "Espace disque bas",
                        "Il ne reste que " + usableMb + " Mo d'espace disque disponible sur le serveur " +
                                "(seuil d'alerte : " + minFreeSpaceMb + " Mo). Les documents joints, les " +
                                "sauvegardes et la base de données partagent ce disque : sans intervention, " +
                                "l'application risque de ne plus pouvoir écrire de nouvelles données."
                );
            }
        } catch (Exception e) {
            log.error("❌ Erreur vérification espace disque : {}", e.getMessage());
        }
    }

    private void checkLastBackup() {
        try {
            BackupConfig config = backupConfigRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
            if (config == null || !config.isAutoEnabled()) {
                return;
            }

            List<BackupInfoDto> backups = backupService.list();
            LocalDateTime mostRecent = backups.stream()
                    .map(BackupInfoDto::getCreatedAt)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            if (mostRecent == null) {
                log.warn("⚠ Sauvegarde automatique activée mais aucune sauvegarde trouvée");
                emailService.sendSystemAlert(
                        "Aucune sauvegarde trouvée",
                        "La sauvegarde automatique quotidienne est activée dans la configuration, mais " +
                                "aucun fichier de sauvegarde n'a été trouvé sur le serveur. Vérifiez les logs " +
                                "applicatifs pour comprendre pourquoi la sauvegarde planifiée n'a pas produit de fichier."
                );
                return;
            }

            Duration since = Duration.between(mostRecent, LocalDateTime.now());
            if (since.toHours() > 26) {
                log.warn("⚠ Dernière sauvegarde vieille de {} heures", since.toHours());
                emailService.sendSystemAlert(
                        "Sauvegarde automatique en retard",
                        "La dernière sauvegarde réussie date de " + since.toHours() + " heures, alors que la " +
                                "sauvegarde automatique quotidienne est activée. Vérifiez que le job planifié " +
                                "s'exécute toujours correctement (espace disque, accès à pg_dump, etc.)."
                );
            }
        } catch (Exception e) {
            log.error("❌ Erreur vérification dernière sauvegarde : {}", e.getMessage());
        }
    }
}
