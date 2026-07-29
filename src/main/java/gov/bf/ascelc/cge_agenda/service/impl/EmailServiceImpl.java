package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;
import gov.bf.ascelc.cge_agenda.entities.EmailOutbox;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.File;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import gov.bf.ascelc.cge_agenda.enums.EmailOutboxStatus;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.FileRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.OrgConfigService;
import gov.bf.ascelc.cge_agenda.utils.EmailTemplateVariables;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final Keycloak keycloakAdminClient;
    private final EmailOutboxService emailOutboxService;
    private final OrgConfigService orgConfigService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;  // ex: http://localhost:4200

    @Value("${app.api-url}")
    private String apiUrl;   // ex: http://localhost:8081

    @Value("${keycloak.admin.realm}")
    private String realmName;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);


    private Context buildBaseContext() {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("baseUrl", baseUrl);
        context.setVariable("apiUrl",  apiUrl);
        return context;
    }

    private Map<String, String> buildVariables(Event event, Participant participant) {
        Map<String, String> vars = new HashMap<>();
        vars.put("evenement", event.getTitle());
        if (participant != null) {
            vars.put("participant", participant.getFirstName() + " " + participant.getLastName());
        }
        vars.put("date_debut", event.getStartDate() != null ? event.getStartDate().format(DATE_FMT) : "");
        vars.put("date_fin", event.getEndDate() != null ? event.getEndDate().format(DATE_FMT) : "");
        vars.put("lieu", event.getVille() != null ? event.getVille() : "");
        return vars;
    }

    private String resolveSubject(String configured, String fallback, Map<String, String> vars) {
        String template = (configured != null && !configured.isBlank()) ? configured : fallback;
        return EmailTemplateVariables.substitute(template, vars);
    }

    private String resolveBody(String configured, String fallback, Map<String, String> vars) {
        String template = (configured != null && !configured.isBlank()) ? configured : fallback;
        return EmailTemplateVariables.substitute(template, vars);
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

            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, participant);

            Context context = buildBaseContext();
            context.setVariable("event",       event);
            context.setVariable("participant", participant);
            context.setVariable("heureDebut",  horaires[0]);
            context.setVariable("heureFin",    horaires[1]);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyInvitation(),
                    "Vous avez été inscrit(e) à l'événement {evenement}. Veuillez en prendre note dans votre agenda.",
                    vars));

            String htmlContent = templateEngine.process("email/invitation", context);
            String subject     = resolveSubject(orgConfig.getSubjectInvitation(),
                    "Invitation : {evenement}", vars);

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

            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event",      event);
            context.setVariable("daysUntil",  daysUntil);
            context.setVariable("heureDebut", horaires[0]);
            context.setVariable("heureFin",   horaires[1]);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyReminder(),
                    "Rappel : l'événement {evenement} approche.", vars));

            String htmlContent = templateEngine.process("email/reminder", context);
            String subject     = resolveSubject(orgConfig.getSubjectReminder(),
                    "Rappel : {evenement}", vars);

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

            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event",      event);
            context.setVariable("heureDebut", horaires[0]);
            context.setVariable("heureFin",   horaires[1]);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyEventUpdate(),
                    "L'événement {evenement} a été modifié. Veuillez consulter les nouvelles informations ci-dessous et mettre à jour votre agenda.",
                    vars));

            String htmlContent = templateEngine.process("email/event-update", context);
            String subject     = resolveSubject(orgConfig.getSubjectEventUpdate(),
                    "Mise à jour : {evenement}", vars);

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

            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("file",  file);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyNewDocument(),
                    "Un nouveau document a été ajouté à l'événement {evenement}. Vous pouvez le consulter et le télécharger dès maintenant.",
                    vars));

            String htmlContent = templateEngine.process("email/new-document", context);
            String subject     = resolveSubject(orgConfig.getSubjectNewDocument(),
                    "Nouveau document : {evenement}", vars);

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
    // DEMANDE DE VALIDATION (CGE/ADMIN)
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendValidationRequest(UUID eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.error("❌ Event introuvable : {}", eventId);
                return;
            }
            List<String> recipients = getCgeAndAdminEmails();
            if (recipients.isEmpty()) {
                log.warn("⚠ Aucun destinataire CGE/ADMIN trouvé pour la demande de validation");
                return;
            }
            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyValidationRequest(),
                    "Un événement a été soumis et attend votre validation.", vars));
            String htmlContent = templateEngine.process("email/validation-request", context);
            String subject = resolveSubject(orgConfig.getSubjectValidationRequest(),
                    "Demande de validation : {evenement}", vars);
            for (String to : recipients) {
                sendEmail(to, subject, htmlContent);
            }
            log.info("✅ Demande de validation envoyée à {} destinataire(s)", recipients.size());
        } catch (Exception e) {
            log.error("❌ Erreur demande de validation eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // ÉVÉNEMENT REJETÉ
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendEventRejected(UUID eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null || event.getCreatorEmail() == null) {
                log.warn("⚠ Impossible de notifier le rejet (event ou créateur introuvable) : {}", eventId);
                return;
            }
            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyRejected(),
                    "Votre événement {evenement} a été rejeté. Le motif est détaillé ci-dessous.", vars));
            String htmlContent = templateEngine.process("email/rejected", context);
            String subject = resolveSubject(orgConfig.getSubjectRejected(),
                    "Événement rejeté : {evenement}", vars);
            sendEmail(event.getCreatorEmail(), subject, htmlContent);
            log.info("✅ Notification de rejet envoyée à {}", event.getCreatorEmail());
        } catch (Exception e) {
            log.error("❌ Erreur notification rejet eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // CORRECTIONS DEMANDÉES
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendChangesRequested(UUID eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null || event.getCreatorEmail() == null) {
                log.warn("⚠ Impossible de notifier les corrections (event ou créateur introuvable) : {}", eventId);
                return;
            }
            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyChangesRequested(),
                    "Le CGE a demandé des corrections sur votre événement {evenement} avant de pouvoir le valider.",
                    vars));
            String htmlContent = templateEngine.process("email/changes-requested", context);
            String subject = resolveSubject(orgConfig.getSubjectChangesRequested(),
                    "Corrections demandées : {evenement}", vars);
            sendEmail(event.getCreatorEmail(), subject, htmlContent);
            log.info("✅ Notification de corrections envoyée à {}", event.getCreatorEmail());
        } catch (Exception e) {
            log.error("❌ Erreur notification corrections eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // CORRECTIONS APPORTÉES (RE-SOUMISSION)
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendAmendmentsCorrected(UUID eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                log.error("❌ Event introuvable : {}", eventId);
                return;
            }
            List<String> recipients = getCgeAndAdminEmails();
            if (recipients.isEmpty()) {
                log.warn("⚠ Aucun destinataire CGE/ADMIN trouvé pour la re-soumission");
                return;
            }
            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyAmendmentsCorrected(),
                    "Le créateur a apporté les corrections demandées. L'événement est de nouveau en attente de votre validation.",
                    vars));
            String htmlContent = templateEngine.process("email/amendments-corrected", context);
            String subject = resolveSubject(orgConfig.getSubjectAmendmentsCorrected(),
                    "Corrections apportées : {evenement}", vars);
            for (String to : recipients) {
                sendEmail(to, subject, htmlContent);
            }
            log.info("✅ Notification de re-soumission envoyée à {} destinataire(s)", recipients.size());
        } catch (Exception e) {
            log.error("❌ Erreur notification re-soumission eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // DÉLÉGATION DE PARTICIPATION
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendDelegationNotice(UUID eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null || event.getDelegueEmail() == null) {
                log.warn("⚠ Impossible de notifier la délégation (event ou délégué introuvable) : {}", eventId);
                return;
            }
            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyDelegation(),
                    "Vous avez été désigné(e) pour représenter le CGE à l'événement suivant :", vars));
            String htmlContent = templateEngine.process("email/delegation", context);
            String subject = resolveSubject(orgConfig.getSubjectDelegation(),
                    "Délégation : {evenement}", vars);
            sendEmail(event.getDelegueEmail(), subject, htmlContent);
            log.info("✅ Notification de délégation envoyée à {}", event.getDelegueEmail());
        } catch (Exception e) {
            log.error("❌ Erreur notification délégation eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // ANNULATION D'ÉVÉNEMENT
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendEventCancellation(UUID eventId, String reason) {
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

            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("reason", reason);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyCancellation(),
                    "L'événement {evenement} a été annulé.", vars));
            String htmlContent = templateEngine.process("email/cancellation", context);
            String subject = resolveSubject(orgConfig.getSubjectCancellation(),
                    "Annulation : {evenement}", vars);

            for (Participant participant : participants) {
                sendEmail(participant.getEmail(), subject, htmlContent);
            }
            log.info("✅ Notifications d'annulation → {} participants pour '{}'",
                    participants.size(), event.getTitle());
        } catch (Exception e) {
            log.error("❌ Erreur notification annulation eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // REPORT D'ÉVÉNEMENT
    // ==========================================
    @Async
    @Override
    @Transactional(readOnly = true)
    public void sendEventPostponement(UUID eventId) {
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

            OrgConfigDto orgConfig = orgConfigService.getConfig();
            Map<String, String> vars = buildVariables(event, null);

            Context context = buildBaseContext();
            context.setVariable("event", event);
            context.setVariable("customMessage", resolveBody(orgConfig.getBodyPostponement(),
                    "L'événement {evenement} a été reporté à une nouvelle date.", vars));
            String htmlContent = templateEngine.process("email/postponement", context);
            String subject = resolveSubject(orgConfig.getSubjectPostponement(),
                    "Report : {evenement}", vars);

            for (Participant participant : participants) {
                sendEmail(participant.getEmail(), subject, htmlContent);
            }
            log.info("✅ Notifications de report → {} participants pour '{}'",
                    participants.size(), event.getTitle());
        } catch (Exception e) {
            log.error("❌ Erreur notification report eventId={} : {}", eventId, e.getMessage());
        }
    }

    // ==========================================
    // DESTINATAIRES CGE/ADMIN (Keycloak)
    // ==========================================
    private List<String> getCgeAndAdminEmails() {
        Set<String> emails = new HashSet<>();
        for (String roleName : new String[]{"CGE", "ADMIN"}) {
            try {
                for (UserRepresentation user : keycloakAdminClient.realm(realmName)
                        .roles().get(roleName).getUserMembers()) {
                    if (user.getEmail() != null && Boolean.TRUE.equals(user.isEnabled())) {
                        emails.add(user.getEmail());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠ Impossible de lister les membres du rôle {} : {}", roleName, e.getMessage());
            }
        }
        return emails.stream().toList();
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
    // ENVOI EMAIL — via file d'attente persistante (outbox)
    // ==========================================
    // Calendrier de nouvelles tentatives : couvre aussi bien une coupure SMTP de
    // quelques secondes (première retentative à 1 min) qu'une panne de plusieurs
    // jours (dernier palier à 24h, répété jusqu'à MAX_ATTEMPTS).
    private static final List<Duration> BACKOFF_SCHEDULE = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15),
            Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2),
            Duration.ofHours(4), Duration.ofHours(8), Duration.ofHours(12),
            Duration.ofHours(24)
    );
    private static final int MAX_ATTEMPTS = 20; // ~12 jours de tentatives au total

    private void sendEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent, null);
    }

    /**
     * Enregistre l'email dans l'outbox (transaction dédiée, indépendante de la
     * transaction readOnly de la méthode appelante) puis tente un envoi immédiat.
     * En cas d'échec (SMTP indisponible), la tentative suivante sera retentée par
     * {@link #processOutbox()} selon {@link #BACKOFF_SCHEDULE} — l'email n'est jamais
     * perdu tant que MAX_ATTEMPTS n'est pas atteint.
     */
    private void sendEmail(String to, String subject, String htmlContent, String context) {
        EmailOutbox item = emailOutboxService.create(to, subject, htmlContent, context);
        attemptDelivery(item);
    }

    /**
     * Retente l'envoi de tous les emails en attente dont l'heure de nouvelle tentative
     * est passée. Appelé périodiquement par {@code EmailOutboxScheduler}.
     */
    @Override
    public void processOutbox() {
        List<EmailOutbox> due = emailOutboxService.findDue();
        if (due.isEmpty()) {
            return;
        }
        log.info("📬 Outbox email : {} envoi(s) à retenter", due.size());
        due.forEach(this::attemptDelivery);
    }

    private void attemptDelivery(EmailOutbox item) {
        try {
            sendRawEmail(item.getRecipientEmail(), item.getSubject(), item.getHtmlContent());
            emailOutboxService.markSent(item);
            log.debug("📧 Email envoyé à : {} (tentative {})", item.getRecipientEmail(), item.getAttempts() + 1);
        } catch (Exception e) {
            int attempts = item.getAttempts() + 1;

            if (attempts >= MAX_ATTEMPTS) {
                emailOutboxService.markFailedAttempt(
                        item, attempts, item.getNextAttemptAt(), EmailOutboxStatus.FAILED, e.getMessage());
                log.error("❌ Email à {} abandonné après {} tentatives : {}",
                        item.getRecipientEmail(), attempts, e.getMessage());
            } else {
                Duration backoff = BACKOFF_SCHEDULE.get(
                        Math.min(attempts - 1, BACKOFF_SCHEDULE.size() - 1));
                LocalDateTime nextAttemptAt = LocalDateTime.now().plus(backoff);
                emailOutboxService.markFailedAttempt(
                        item, attempts, nextAttemptAt, EmailOutboxStatus.PENDING, e.getMessage());
                log.warn("⚠ Échec envoi à {} (tentative {}/{}), nouvelle tentative dans {} : {}",
                        item.getRecipientEmail(), attempts, MAX_ATTEMPTS, backoff, e.getMessage());
            }
        }
    }

    private void sendRawEmail(String to, String subject, String htmlContent) throws MessagingException {
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
    }
}