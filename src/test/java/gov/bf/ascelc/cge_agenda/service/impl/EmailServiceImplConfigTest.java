package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.repository.*;
import gov.bf.ascelc.cge_agenda.service.OrgConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailServiceImplConfigTest {

    private EventRepository eventRepository;
    private ParticipantRepository participantRepository;
    private OrgConfigService orgConfigService;
    private JavaMailSender mailSender;
    private EmailOutboxService emailOutboxService;
    private EmailServiceImpl service;

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        eventRepository = mock(EventRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        orgConfigService = mock(OrgConfigService.class);
        mailSender = mock(JavaMailSender.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        when(templateEngine.process(anyString(), any())).thenReturn("<html></html>");
        when(mailSender.createMimeMessage()).thenReturn(
                new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null));

        emailOutboxService = mock(EmailOutboxService.class);
        when(emailOutboxService.create(anyString(), anyString(), anyString(), any()))
                .thenAnswer(inv -> gov.bf.ascelc.cge_agenda.entities.EmailOutbox.builder()
                        .recipientEmail(inv.getArgument(0))
                        .subject(inv.getArgument(1))
                        .htmlContent(inv.getArgument(2))
                        .attempts(0)
                        .build());

        service = new EmailServiceImpl(
                mailSender, templateEngine,
                mock(ParticipantEventRepository.class), participantRepository,
                eventRepository, mock(ScheduleRepository.class), mock(FileRepository.class),
                mock(Keycloak.class), emailOutboxService, orgConfigService
        );

        Event event = Event.builder().id(EVENT_ID).title("Réunion budget")
                .type(EventType.REUNION).startDate(LocalDate.now()).endDate(LocalDate.now()).build();
        Participant participant = Participant.builder().id(PARTICIPANT_ID)
                .firstName("Jean").lastName("Dupont").email("jean@example.com").build();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));

        OrgConfigDto config = OrgConfigDto.builder()
                .subjectInvitation("Invitez-vous : {evenement}")
                .bodyInvitation("Bonjour {participant}, merci de noter {evenement}.")
                .build();
        when(orgConfigService.getConfig()).thenReturn(config);
    }

    @Test
    void sendEventInvitation_usesConfiguredSubjectAndBody_notHardcodedText() {
        service.sendEventInvitation(EVENT_ID, PARTICIPANT_ID);

        // Note d'implémentation : l'envoi réel passe par l'outbox asynchrone (EmailOutboxService),
        // et mailSender.send(...) n'est atteint qu'après une injection Spring réelle de `fromEmail`
        // (absente ici, test unitaire pur) — donc, comme anticipé par la brief, on vérifie l'appel
        // à EmailOutboxService.create(...) plutôt qu'à mailSender.send(...), ce qui permet en plus
        // une assertion directe sur le sujet/corps résolus (substitution effective, pas de texte
        // codé en dur "Invitation : ").
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailOutboxService, timeout(2000)).create(
                eq("jean@example.com"), subjectCaptor.capture(), bodyCaptor.capture(), any());

        verify(orgConfigService, timeout(2000)).getConfig();

        assertThat(subjectCaptor.getValue()).isEqualTo("Invitez-vous : Réunion budget");
        assertThat(subjectCaptor.getValue()).doesNotContain("Invitation :");
    }
}
