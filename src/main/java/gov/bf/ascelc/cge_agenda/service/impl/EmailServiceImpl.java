package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.File;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.FileRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final ParticipantEventRepository participantEventRepository;
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final ScheduleRepository scheduleRepository;
    private final FileRepository fileRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;  // ex: http://localhost:4200

    @Value("${app.api-url}")
    private String apiUrl;   // ex: http://localhost:8081

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");


    private Context buildBaseContext() {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("apiUrl",  apiUrl);
        return context;
    }

    // ==========================================
    // INVITATION (NOUVEAU PARTICIPANT)
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendEventInvitation(UUID eventId, UUID participantId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.error("❌ Event introuvable : {}", eventId);
                return;
            }

            Participant participant = participantRepository.findById(participantId)
                    .orElse(null);
            if (participant == null) {
                log.error("❌ Participant introuvable : {}", participantId);
                return;
            }

            log.info("sendEventInvitation → title='{}' | participant='{} {}'",
                    event.getTitle(),
                    participant.getFirstName(), participant.getLastName());

            String[] horaires = getHoraires(eventId);

            Context context = buildBaseContext();
            context.setVariable("event",       event);
            context.setVariable("participant", participant);
            context.setVariable("heureDebut",  horaires[0]);
            context.setVariable("heureFin",    horaires[1]);

            String htmlContent = templateEngine.process("email/invitation", context);
            String subject     = "Invitation : " + event.getTitle();

            sendEmail(participant.getEmail(), subject, htmlContent);
            log.info("✅ Invitation envoyée à {}", participant.getEmail());

        } catch (Exception e) {
            log.error("❌ Erreur invitation eventId={} participantId={} : {}",
                    eventId, participantId, e.getMessage());
        }
    }

    // ==========================================
    // RAPPEL J-7 / J-1
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendEventReminder(UUID eventId, int daysUntil) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.error("❌ Event introuvable : {}", eventId);
                return;
            }

            List<Participant> participants =
                    participantEventRepository.findParticipantsByEventId(eventId);

            if (participants.isEmpty()) {
                log.warn("⚠ Aucun participant pour l'événement : {}", eventId);
                return;
            }

            String[] horaires = getHoraires(eventId);

            Context context = buildBaseContext();
            context.setVariable("event",      event);
            context.setVariable("daysUntil",  daysUntil);
            context.setVariable("heureDebut", horaires[0]);
            context.setVariable("heureFin",   horaires[1]);

            String htmlContent = templateEngine.process("email/reminder", context);
            String subject     = "Rappel J-" + daysUntil + " : " + event.getTitle();

            for (Participant participant : participants) {
                sendEmail(participant.getEmail(), subject, htmlContent);
            }
            log.info("✅ Rappels J-{} envoyés à {} participants pour '{}'",
                    daysUntil, participants.size(), event.getTitle());

        } catch (Exception e) {
            log.error("❌ Erreur rappel J-{} eventId={} : {}",
                    daysUntil, eventId, e.getMessage());
        }
    }

    // ==========================================
    // NOTIFICATION MODIFICATION
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendEventUpdateNotification(UUID eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.error("❌ Event introuvable : {}", eventId);
                return;
            }

            List<Participant> participants =
                    participantEventRepository.findParticipantsByEventId(eventId);

            if (participants.isEmpty()) {
                log.warn("⚠ Aucun participant pour l'événement : {}", eventId);
                return;
            }

            String[] horaires = getHoraires(eventId);

            Context context = buildBaseContext();
            context.setVariable("event",      event);
            context.setVariable("heureDebut", horaires[0]);
            context.setVariable("heureFin",   horaires[1]);

            String htmlContent = templateEngine.process("email/event-update", context);
            String subject     = "Modification : " + event.getTitle();

            for (Participant participant : participants) {
                sendEmail(participant.getEmail(), subject, htmlContent);
            }
            log.info("✅ Notifications modification → {} participants pour '{}'",
                    participants.size(), event.getTitle());

        } catch (Exception e) {
            log.error("❌ Erreur modification eventId={} : {}",
                    eventId, e.getMessage());
        }
    }

    // ==========================================
    // NOUVEAU DOCUMENT
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendNewDocumentNotification(UUID eventId, UUID fileId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.error("❌ Event introuvable : {}", eventId);
                return;
            }

            File file = fileRepository.findById(fileId).orElse(null);
            if (file == null) {
                log.error("❌ File introuvable : {}", fileId);
                return;
            }

            List<Participant> participants =
                    participantEventRepository.findParticipantsByEventId(eventId);

            if (participants.isEmpty()) {
                log.warn("⚠ Aucun participant pour l'événement : {}", eventId);
                return;
            }

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("file",  file);

            String htmlContent = templateEngine.process("email/new-document", context);
            String subject     = "Nouveau document : " + event.getTitle();

            for (Participant participant : participants) {
                sendEmail(participant.getEmail(), subject, htmlContent);
            }
            log.info("✅ Notifications nouveau document → {} participants",
                    participants.size());

        } catch (Exception e) {
            log.error("❌ Erreur document eventId={} : {}",
                    eventId, e.getMessage());
        }
    }

    // ==========================================
    // HORAIRES — charge le premier schedule
    // ==========================================
    private String[] getHoraires(UUID eventId) {
        try {
            List<Schedule> schedules = scheduleRepository.findByEventId(eventId);
            if (!schedules.isEmpty()) {
                Schedule first = schedules.get(0);
                return new String[]{
                        first.getStartTime().format(TIME_FMT),
                        first.getEndTime().format(TIME_FMT)
                };
            }
        } catch (Exception e) {
            log.warn("⚠ Horaires introuvables pour eventId={} : {}",
                    eventId, e.getMessage());
        }
        return new String[]{"", ""};
    }

    // ==========================================
    // ENVOI EMAIL — avec logo inline
    // ==========================================
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            try {
                ClassPathResource logo =
                        new ClassPathResource("static/images/logo.png");
                if (logo.exists()) {
                    helper.addInline("logo", logo);
                }
            } catch (Exception e) {
                log.warn("⚠ Logo non trouvé, email envoyé sans logo");
            }

            mailSender.send(message);
            log.debug("📧 Email envoyé à : {}", to);

        } catch (MessagingException e) {
            log.error("❌ Erreur envoi email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur envoi email", e);
        }
    }
}