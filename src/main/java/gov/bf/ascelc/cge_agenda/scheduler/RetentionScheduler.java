package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Purge automatique de la corbeille (événements déjà mis à la corbeille par un utilisateur,
 * jamais des événements actifs) après le délai de rétention configuré. DÉSACTIVÉ par défaut
 * (app.retention.corbeille-purge-days=0) : un organisme public décide lui-même de sa durée de
 * conservation, on ne présume pas d'une valeur. Une alerte est envoyée à chaque purge réelle —
 * jamais silencieuse.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionScheduler {

    private final EventRepository eventRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final ScheduleRepository scheduleRepository;
    private final EmailService emailService;

    @Value("${app.retention.corbeille-purge-days:0}")
    private int corbeillePurgeDays;

    @Scheduled(cron = "0 30 6 * * *")
    @Transactional
    public void purgeCorbeille() {
        if (corbeillePurgeDays <= 0) {
            return;
        }

        LocalDateTime seuil = LocalDateTime.now().minusDays(corbeillePurgeDays);
        List<Event> aPurger = eventRepository.findAllDeleted().stream()
                .filter(e -> e.getUpdatedAt() != null && e.getUpdatedAt().isBefore(seuil))
                .toList();

        if (aPurger.isEmpty()) {
            return;
        }

        int count = 0;
        for (Event event : aPurger) {
            try {
                purgerDefinitivement(event);
                count++;
            } catch (Exception e) {
                log.error("❌ Échec purge rétention événement {} : {}", event.getId(), e.getMessage());
            }
        }

        log.info("✓ Purge rétention : {} événement(s) supprimé(s) définitivement (> {} jours en corbeille)",
                count, corbeillePurgeDays);
        emailService.sendSystemAlert(
                "Purge automatique de la corbeille",
                count + " événement(s) présent(s) dans la corbeille depuis plus de " + corbeillePurgeDays +
                        " jours ont été supprimés définitivement (politique de rétention configurée)."
        );
    }

    private void purgerDefinitivement(Event event) {
        if (!event.getFiles().isEmpty()) {
            event.getFiles().forEach(file -> {
                try {
                    Files.deleteIfExists(Paths.get(file.getFilePath()));
                } catch (Exception e) {
                    log.warn("⚠ Fichier physique non supprimé ({}) : {}", file.getFileName(), e.getMessage());
                }
            });
        }
        if (!event.getParticipantEvents().isEmpty()) {
            participantEventRepository.deleteAll(event.getParticipantEvents());
        }
        if (!event.getSchedules().isEmpty()) {
            scheduleRepository.deleteAll(event.getSchedules());
        }
        eventRepository.deleteById(event.getId());
    }
}
