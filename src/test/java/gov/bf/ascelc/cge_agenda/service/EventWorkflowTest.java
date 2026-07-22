package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.impl.EventServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Vérifie les gardes de transition du workflow de validation CGE (BROUILLON →
 * EN_ATTENTE_VALIDATION → PLANIFIE/A_CORRIGER/REJETE, etc.) — la logique la plus
 * récente et la plus sensible de l'application : un mauvais garde-fou permettrait
 * de faire sauter une étape de validation.
 */
class EventWorkflowTest {

    private EventRepository eventRepository;
    private EventMapper eventMapper;
    private EmailService emailService;
    private EventServiceImpl service;

    private static final UUID EVENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventRepository = mock(EventRepository.class);
        eventMapper = mock(EventMapper.class);
        ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
        ParticipantRepository participantRepository = mock(ParticipantRepository.class);
        ParticipantEventRepository participantEventRepository = mock(ParticipantEventRepository.class);
        ParticipantMapper participantMapper = mock(ParticipantMapper.class);
        emailService = mock(EmailService.class);

        service = new EventServiceImpl(
                eventRepository, eventMapper, scheduleRepository,
                participantRepository, participantEventRepository,
                participantMapper, emailService
        );

        lenient().when(eventMapper.toDto(any(Event.class))).thenReturn(EventDto.builder().build());

        // Les méthodes de workflow enregistrent un callback post-commit (envoi d'email) via
        // TransactionSynchronizationManager — hors d'un vrai contexte Spring @Transactional,
        // cela nécessite d'activer manuellement la synchronisation de transaction.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    private Event eventWithStatus(EventStatus status) {
        return Event.builder()
                .id(EVENT_ID)
                .title("Test")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .status(status)
                .build();
    }

    private void mockFind(Event event) {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==========================================
    // SUBMIT : uniquement depuis BROUILLON
    // ==========================================
    @Test
    void submitDraft_fromBrouillon_succeedsAndMovesToEnAttente() {
        Event event = eventWithStatus(EventStatus.BROUILLON);
        mockFind(event);

        service.submitDraft(EVENT_ID);

        assertThat(event.getStatus()).isEqualTo(EventStatus.EN_ATTENTE_VALIDATION);
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"BROUILLON"}, mode = EnumSource.Mode.EXCLUDE)
    void submitDraft_fromAnyOtherStatus_rejected(EventStatus status) {
        mockFind(eventWithStatus(status));

        assertThatThrownBy(() -> service.submitDraft(EVENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("brouillon");
    }

    // ==========================================
    // VALIDATE : uniquement depuis EN_ATTENTE_VALIDATION
    // ==========================================
    @Test
    void validateEvent_fromEnAttente_succeedsAndMovesToPlanifie() {
        Event event = eventWithStatus(EventStatus.EN_ATTENTE_VALIDATION);
        mockFind(event);

        service.validateEvent(EVENT_ID, "ok");

        assertThat(event.getStatus()).isEqualTo(EventStatus.PLANIFIE);
        assertThat(event.getValidationComment()).isEqualTo("ok");
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"EN_ATTENTE_VALIDATION"}, mode = EnumSource.Mode.EXCLUDE)
    void validateEvent_fromAnyOtherStatus_rejected(EventStatus status) {
        mockFind(eventWithStatus(status));

        assertThatThrownBy(() -> service.validateEvent(EVENT_ID, "ok"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ==========================================
    // REJECT : depuis EN_ATTENTE_VALIDATION ou A_CORRIGER
    // ==========================================
    @Test
    void rejectEvent_fromEnAttente_succeeds() {
        Event event = eventWithStatus(EventStatus.EN_ATTENTE_VALIDATION);
        mockFind(event);

        service.rejectEvent(EVENT_ID, "budget insuffisant");

        assertThat(event.getStatus()).isEqualTo(EventStatus.REJETE);
        assertThat(event.getRejectionReason()).isEqualTo("budget insuffisant");
    }

    @Test
    void rejectEvent_fromACorriger_succeeds() {
        Event event = eventWithStatus(EventStatus.A_CORRIGER);
        mockFind(event);

        service.rejectEvent(EVENT_ID, "toujours incomplet");

        assertThat(event.getStatus()).isEqualTo(EventStatus.REJETE);
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"EN_ATTENTE_VALIDATION", "A_CORRIGER"}, mode = EnumSource.Mode.EXCLUDE)
    void rejectEvent_fromAnyOtherStatus_rejected(EventStatus status) {
        mockFind(eventWithStatus(status));

        assertThatThrownBy(() -> service.rejectEvent(EVENT_ID, "raison"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ==========================================
    // REQUEST CHANGES : uniquement depuis EN_ATTENTE_VALIDATION
    // ==========================================
    @Test
    void requestChanges_fromEnAttente_movesToACorriger() {
        Event event = eventWithStatus(EventStatus.EN_ATTENTE_VALIDATION);
        mockFind(event);

        service.requestChanges(EVENT_ID, "préciser le lieu");

        assertThat(event.getStatus()).isEqualTo(EventStatus.A_CORRIGER);
        assertThat(event.getChangeSuggestions()).isEqualTo("préciser le lieu");
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"EN_ATTENTE_VALIDATION"}, mode = EnumSource.Mode.EXCLUDE)
    void requestChanges_fromAnyOtherStatus_rejected(EventStatus status) {
        mockFind(eventWithStatus(status));

        assertThatThrownBy(() -> service.requestChanges(EVENT_ID, "x"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ==========================================
    // OBSERVATION / DÉLÉGATION : uniquement PLANIFIE ou EN_COURS
    // ==========================================
    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"PLANIFIE", "EN_COURS"})
    void addObservation_whenOperational_succeeds(EventStatus status) {
        Event event = eventWithStatus(status);
        mockFind(event);

        service.addObservation(EVENT_ID, "tout va bien");

        assertThat(event.getValidationComment()).isEqualTo("tout va bien");
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"PLANIFIE", "EN_COURS"}, mode = EnumSource.Mode.EXCLUDE)
    void addObservation_whenNotOperational_rejected(EventStatus status) {
        mockFind(eventWithStatus(status));

        assertThatThrownBy(() -> service.addObservation(EVENT_ID, "x"))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ==========================================
    // COMPTE-RENDU : uniquement TERMINE, et CGE/ADMIN ou créateur
    // ==========================================
    @Test
    void saveCompteRendu_whenTermineAndCge_succeeds() {
        Event event = eventWithStatus(EventStatus.TERMINE);
        mockFind(event);
        authenticateAs("cge@ascelc.bf", "CGE");

        service.saveCompteRendu(EVENT_ID, "points", "décisions", "actions");

        assertThat(event.getCompteRenduPoints()).isEqualTo("points");
        assertThat(event.getCompteRenduRedigePar()).isEqualTo("cge@ascelc.bf");
    }

    @Test
    void saveCompteRendu_whenTermineAndCreator_succeeds() {
        Event event = eventWithStatus(EventStatus.TERMINE);
        event.setCreatorEmail("createur@ascelc.bf");
        mockFind(event);
        authenticateAs("createur@ascelc.bf", "PROTOCOLE");

        service.saveCompteRendu(EVENT_ID, "points", "décisions", "actions");

        assertThat(event.getCompteRenduPoints()).isEqualTo("points");
    }

    @Test
    void saveCompteRendu_whenNeitherCgeNorCreator_forbidden() {
        Event event = eventWithStatus(EventStatus.TERMINE);
        event.setCreatorEmail("createur@ascelc.bf");
        mockFind(event);
        authenticateAs("quelquun.dautre@ascelc.bf", "PROTOCOLE");

        assertThatThrownBy(() -> service.saveCompteRendu(EVENT_ID, "p", "d", "a"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void saveCompteRendu_whenNotTermine_rejectedRegardlessOfRole() {
        mockFind(eventWithStatus(EventStatus.PLANIFIE));
        authenticateAs("cge@ascelc.bf", "CGE");

        assertThatThrownBy(() -> service.saveCompteRendu(EVENT_ID, "p", "d", "a"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("terminé");
    }

    private void authenticateAs(String email, String role) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("email")).thenReturn(email);
        Authentication auth = new TestingAuthenticationToken(jwt, null, "ROLE_" + role);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
