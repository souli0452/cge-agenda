package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.SchedulerConfig;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.SchedulerConfigRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final List<Integer> DEFAULT_REMINDER_DAYS = List.of(7, 1);
    private static final int DEFAULT_SEND_HOUR = 7;

    private final EventRepository eventRepository;
    private final EmailService emailService;
    private final SchedulerConfigRepository schedulerConfigRepository;

    private final Set<String> sentToday = ConcurrentHashMap.newKeySet();
    private LocalDate lastResetDate = LocalDate.now();

    /**
     * Vérifie chaque heure si c'est l'heure configurée pour l'envoi des rappels.
     * L'heure d'envoi est configurable (admin/scheduler) sans nécessiter de redémarrage.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkAndSendDailyReminders() {
        SchedulerConfig config = schedulerConfigRepository.findFirstByOrderByCreatedAtAsc().orElse(null);

        boolean enabled = config == null || config.isReminderEnabled();
        int sendHour = config != null ? config.getSendHour() : DEFAULT_SEND_HOUR;

        if (!enabled) {
            return;
        }
        if (LocalTime.now().getHour() != sendHour) {
            return;
        }

        List<Integer> reminderDays = (config != null && config.getReminderDays() != null && !config.getReminderDays().isEmpty())
                ? config.getReminderDays()
                : DEFAULT_REMINDER_DAYS;

        sendDailyReminders(reminderDays);
    }

    /**
     * Déclenche immédiatement l'envoi des rappels (bouton "Exécuter maintenant"),
     * sans tenir compte de l'heure configurée ni du dédoublonnage journalier.
     */
    public String runNow() {
        SchedulerConfig config = schedulerConfigRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        List<Integer> reminderDays = (config != null && config.getReminderDays() != null && !config.getReminderDays().isEmpty())
                ? config.getReminderDays()
                : DEFAULT_REMINDER_DAYS;

        log.info("=== Déclenchement manuel des rappels ===");
        int total = 0;
        for (int daysUntil : reminderDays) {
            total += sendRemindersForDay(daysUntil, false);
        }
        log.info("=== Déclenchement manuel terminé : {} rappel(s) envoyé(s) ===", total);
        return total + " rappel(s) envoyé(s)";
    }

    private void sendDailyReminders(List<Integer> reminderDays) {
        log.info("=== Début envoi des rappels quotidiens ===");

        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            sentToday.clear();
            lastResetDate = today;
        }

        for (int daysUntil : reminderDays) {
            sendRemindersForDay(daysUntil, true);
        }

        log.info("=== Rappels quotidiens terminés ===");
    }

    private int sendRemindersForDay(int daysUntil, boolean dedupe) {
        LocalDate targetDate = LocalDate.now().plusDays(daysUntil);

        List<Event> events = eventRepository
                .findByStartDateAndStatus(targetDate, EventStatus.PLANIFIE);

        log.info("J-{} : {} événement(s) trouvé(s) pour le {}",
                daysUntil, events.size(), targetDate);

        int[] sentCount = {0};
        events.forEach(event -> {
            String key = event.getId() + "-" + daysUntil;
            if (dedupe && sentToday.contains(key)) {
                log.info("⏭ Rappel J-{} déjà envoyé aujourd'hui → {}",
                        daysUntil, event.getTitle());
                return;
            }
            try {
                emailService.sendEventReminder(event.getId(), daysUntil);
                sentToday.add(key);
                sentCount[0]++;
                log.info("✅ Rappel J-{} envoyé → {}", daysUntil, event.getTitle());
            } catch (Exception e) {
                log.error("❌ Échec rappel J-{} pour '{}' : {}",
                        daysUntil, event.getTitle(), e.getMessage());
            }
        });
        return sentCount[0];
    }
}