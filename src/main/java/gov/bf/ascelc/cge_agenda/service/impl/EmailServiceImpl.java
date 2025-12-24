package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.File;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final ParticipantEventRepository participantEventRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendEventReminder(Event event, int daysUntil) {
        log.info("Envoi rappel J-{} pour l'événement : {}", daysUntil, event.getTitle());

        try {
            // Récupérer les participants
            List<Participant> participants = participantEventRepository
                    .findParticipantsByEventId(event.getId());

            if (participants.isEmpty()) {
                log.warn("Aucun participant pour l'événement : {}", event.getId());
                return;
            }

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("event", event);
            context.setVariable("daysUntil", daysUntil);

            // Générer le HTML depuis le template
            String htmlContent = templateEngine.process("email/reminder", context);

            // Envoyer à chaque participant
            for (Participant participant : participants) {
                sendEmail(
                        participant.getEmail(),
                        "Rappel : " + event.getTitle() + " (J-" + daysUntil + ")",
                        htmlContent,
                        true
                );
            }

            log.info("Rappels envoyés à {} participants", participants.size());

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des rappels : {}", e.getMessage());
        }
    }

    @Override
    public void sendEventUpdateNotification(Event event) {
        log.info("Envoi notification modification pour : {}", event.getTitle());

        try {
            List<Participant> participants = participantEventRepository
                    .findParticipantsByEventId(event.getId());

            if (participants.isEmpty()) {
                log.warn("Aucun participant pour l'événement : {}", event.getId());
                return;
            }

            Context context = new Context();
            context.setVariable("event", event);

            String htmlContent = templateEngine.process("email/event-update", context);

            for (Participant participant : participants) {
                sendEmail(
                        participant.getEmail(),
                        "Modification : " + event.getTitle(),
                        htmlContent,
                        true
                );
            }

            log.info("Notifications envoyées à {} participants", participants.size());

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications : {}", e.getMessage());
        }
    }

    @Override
    public void sendNewDocumentNotification(Event event, File file) {
        log.info("Envoi notification nouveau document pour : {}", event.getTitle());

        try {
            List<Participant> participants = participantEventRepository
                    .findParticipantsByEventId(event.getId());

            if (participants.isEmpty()) {
                log.warn("Aucun participant pour l'événement : {}", event.getId());
                return;
            }

            Context context = new Context();
            context.setVariable("event", event);
            context.setVariable("file", file);

            String htmlContent = templateEngine.process("email/new-document", context);

            for (Participant participant : participants) {
                sendEmail(
                        participant.getEmail(),
                        "Nouveau document : " + event.getTitle(),
                        htmlContent,
                        true
                );
            }

            log.info("Notifications envoyées à {} participants", participants.size());

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications : {}", e.getMessage());
        }
    }

    /**
     * Méthode privée pour envoyer un email
     */
    private void sendEmail(String to, String subject, String htmlContent, boolean withLogo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Ajouter le logo en pièce jointe inline
            if (withLogo) {
                try {
                    ClassPathResource logo = new ClassPathResource("static/images/logo.png");
                    if (logo.exists()) {
                        helper.addInline("logo", logo);
                    }
                } catch (Exception e) {
                    log.warn("Logo non trouvé, email envoyé sans logo");
                }
            }

            mailSender.send(message);
            log.debug("Email envoyé à : {}", to);

        } catch (MessagingException e) {
            log.error("Erreur envoi email à {} : {}", to, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
}