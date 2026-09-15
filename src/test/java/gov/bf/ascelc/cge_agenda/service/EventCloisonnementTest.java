package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;
import gov.bf.ascelc.cge_agenda.entities.Espace;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantMapper;
import gov.bf.ascelc.cge_agenda.repository.EspaceRepository;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.EventTypeSlaRepository;
import gov.bf.ascelc.cge_agenda.repository.JourFerieRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.impl.EventServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Vérifie le cloisonnement entre espaces : sans le garde-fou assertAccessible (ajouté suite
 * à l'audit de sécurité), n'importe quel utilisateur authentifié avec un rôle métier pouvait
 * agir sur l'événement d'un AUTRE espace en connaissant/devinant son UUID (IDOR). Ce test
 * couvre un échantillon représentatif des méthodes concernées, pas les 13 exhaustivement :
 * elles partagent toutes le même garde-fou (assertAccessible), donc ce sont surtout des
 * variations de câblage plutôt que des chemins de logique distincts.
 */
class EventCloisonnementTest {

    private EventRepository eventRepository;
    private EventMapper eventMapper;
    private EspaceService espaceService;
    private EventServiceImpl service;

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID MON_ESPACE_ID = UUID.randomUUID();
    private static final UUID AUTRE_ESPACE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
        ParticipantRepository participantRepository = mock(ParticipantRepository.class);
        ParticipantEventRepository participantEventRepository = mock(ParticipantEventRepository.class);
        ParticipantMapper participantMapper = mock(ParticipantMapper.class);
        EmailService emailService = mock(EmailService.class);
        AuditService auditService = mock(AuditService.class);
        NotificationService notificationService = mock(NotificationService.class);
        EventTypeSlaRepository eventTypeSlaRepository = mock(EventTypeSlaRepository.class);
        JourFerieRepository jourFerieRepository = mock(JourFerieRepository.class);
        OrgConfigService orgConfigService = mock(OrgConfigService.class);
        EspaceRepository espaceRepository = mock(EspaceRepository.class);
        espaceService = mock(EspaceService.class);

        lenient().when(orgConfigService.getConfig()).thenReturn(OrgConfigDto.builder().build());

        service = new EventServiceImpl(
                eventRepository, eventMapper, scheduleRepository,
                participantRepository, participantEventRepository,
                participantMapper, emailService, auditService, notificationService,
                eventTypeSlaRepository, jourFerieRepository, orgConfigService,
                espaceRepository, espaceService
        );

        lenient().when(eventMapper.toDto(any(Event.class))).thenReturn(EventDto.builder().build());
        TransactionSynchronizationManager.initSynchronization();

        // Utilisateur métier (non-ADMIN) membre uniquement de MON_ESPACE_ID.
        authenticateAs("secretaire@ascelc.bf", "SECRETAIRE");
        lenient().when(espaceService.espacesAccessibles("secretaire@ascelc.bf"))
                .thenReturn(List.of(MON_ESPACE_ID));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    private Event eventInEspace(UUID espaceId, EventStatus status) {
        return Event.builder()
                .id(EVENT_ID)
                .title("Test")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .status(status)
                .espace(Espace.builder().id(espaceId).chefEmail("chef@ascelc.bf").nom("Espace").build())
                .build();
    }

    private void mockFind(Event event) {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void authenticateAs(String email, String role) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("email")).thenReturn(email);
        Authentication auth = new TestingAuthenticationToken(jwt, null, "ROLE_" + role);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    // ==========================================
    // cancelEvent
    // ==========================================
    @Test
    void cancelEvent_whenEventInAutreEspace_returns404() {
        mockFind(eventInEspace(AUTRE_ESPACE_ID, EventStatus.PLANIFIE));

        assertThatThrownBy(() -> service.cancelEvent(EVENT_ID, "raison"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelEvent_whenEventInMonEspace_succeeds() {
        Event event = eventInEspace(MON_ESPACE_ID, EventStatus.PLANIFIE);
        mockFind(event);

        service.cancelEvent(EVENT_ID, "raison");

        assertThat(event.getStatus()).isEqualTo(EventStatus.ANNULER);
    }

    // ==========================================
    // update
    // ==========================================
    @Test
    void update_whenEventInAutreEspace_returns404() {
        mockFind(eventInEspace(AUTRE_ESPACE_ID, EventStatus.PLANIFIE));

        assertThatThrownBy(() -> service.update(EVENT_ID, EventDto.builder()
                .startDate(LocalDate.now()).endDate(LocalDate.now()).build()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==========================================
    // deleteEventPermanently — le cas le plus sensible : suppression définitive + fichiers
    // ==========================================
    @Test
    void deleteEventPermanently_whenEventInAutreEspace_returns404() {
        mockFind(eventInEspace(AUTRE_ESPACE_ID, EventStatus.REJETE));

        assertThatThrownBy(() -> service.deleteEventPermanently(EVENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==========================================
    // getEventParticipants — fuite de lecture (liste de participants d'un autre espace)
    // ==========================================
    @Test
    void getEventParticipants_whenEventInAutreEspace_returns404() {
        mockFind(eventInEspace(AUTRE_ESPACE_ID, EventStatus.PLANIFIE));

        assertThatThrownBy(() -> service.getEventParticipants(EVENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==========================================
    // ADMIN outrepasse le cloisonnement (accès de secours transverse voulu)
    // ==========================================
    @Test
    void cancelEvent_whenAdmin_bypassesCloisonnement() {
        authenticateAs("admin@ascelc.bf", "ADMIN");
        Event event = eventInEspace(AUTRE_ESPACE_ID, EventStatus.PLANIFIE);
        mockFind(event);

        service.cancelEvent(EVENT_ID, "raison");

        assertThat(event.getStatus()).isEqualTo(EventStatus.ANNULER);
    }
}
