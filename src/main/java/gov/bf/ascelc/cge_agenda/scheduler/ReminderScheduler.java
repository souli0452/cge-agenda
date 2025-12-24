package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final EventRepository eventRepository;
    private final EmailService emailService;

    /**
     * S'exécute tous les jours à 9h00 du matin
     * Cron: "seconde minute heure jour mois jour_semaine"
     * "0 0 9 * * *" = à 9h00 tous les jours
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReminders() {
        log.info("=== Début envoi des rappels quotidiens ===");

        try {
            // Rappel J-7 (7 jours avant)
            sendRemindersForDay(7);

            // Rappel J-1 (1 jour avant)
            sendRemindersForDay(1);

            log.info("=== Rappels quotidiens envoyés avec succès ===");
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des rappels quotidiens : {}", e.getMessage());
        }
    }

    /**
     * Envoyer les rappels pour un nombre de jours donné
     */
    private void sendRemindersForDay(int daysUntil) {
        LocalDate targetDate = LocalDate.now().plusDays(daysUntil);

        List<Event> events = eventRepository.findAll().stream()
                .filter(e -> e.getStartDate().equals(targetDate))
                .filter(e -> e.getStatus() == EventStatus.PLANIFIE)
                .toList();

        log.info("Envoi de {} rappels J-{}", events.size(), daysUntil);

        events.forEach(event -> {
            try {
                emailService.sendEventReminder(event, daysUntil);
                log.info("Rappel J-{} envoyé pour : {}", daysUntil, event.getTitle());
            } catch (Exception e) {
                log.error("Erreur envoi rappel pour événement {} : {}",
                        event.getId(), e.getMessage());
            }
        });
    }

    /**
     * Méthode de test - À supprimer en production
     * S'exécute toutes les minutes pour tester
     */
     //@Scheduled(cron = "0 * * * * *")  // Toutes les minutes
     public void testReminders() {
         log.info("TEST - Envoi rappels pour tous les événements planifiés");

         // Récupérer TOUS les événements planifiés
         List<Event> events = eventRepository.findAll().stream()
                 .filter(e -> e.getStatus() == EventStatus.PLANIFIE)
                 .toList();

         log.info("Envoi de {} rappels de test", events.size());

         events.forEach(event -> {
             try {
                 emailService.sendEventReminder(event, 7);  // Simuler J-7
                 log.info("Rappel envoyé pour : {}", event.getTitle());
             } catch (Exception e) {
                 log.error("Erreur envoi rappel : {}", e.getMessage());
             }
         });
     }
}