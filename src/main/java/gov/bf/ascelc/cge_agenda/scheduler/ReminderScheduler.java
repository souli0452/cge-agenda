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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final EventRepository eventRepository;
    private final EmailService emailService;

    private final Set<String> sentToday = ConcurrentHashMap.newKeySet();
    private LocalDate lastResetDate = LocalDate.now();

    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyReminders() {
        log.info("=== Début envoi des rappels quotidiens ===");

        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            sentToday.clear();
            lastResetDate = today;
        }

        sendRemindersForDay(7);
        sendRemindersForDay(1);

        log.info("=== Rappels quotidiens terminés ===");
    }

    private void sendRemindersForDay(int daysUntil) {
        LocalDate targetDate = LocalDate.now().plusDays(daysUntil);

        List<Event> events = eventRepository
                .findByStartDateAndStatus(targetDate, EventStatus.PLANIFIE);

        log.info("J-{} : {} événement(s) trouvé(s) pour le {}",
                daysUntil, events.size(), targetDate);

        events.forEach(event -> {
            String key = event.getId() + "-" + daysUntil;
            if (sentToday.contains(key)) {
                log.info("⏭ Rappel J-{} déjà envoyé aujourd'hui → {}",
                        daysUntil, event.getTitle());
                return;
            }
            try {
                emailService.sendEventReminder(event.getId(), daysUntil);
                sentToday.add(key);
                log.info("✅ Rappel J-{} envoyé → {}", daysUntil, event.getTitle());
            } catch (Exception e) {
                log.error("❌ Échec rappel J-{} pour '{}' : {}",
                        daysUntil, event.getTitle(), e.getMessage());
            }
        });
    }
}