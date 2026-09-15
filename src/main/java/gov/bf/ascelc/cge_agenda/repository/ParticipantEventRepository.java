package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.ParticipantEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection; // ✅ IMPORT MANQUANT
import java.util.List;
import java.util.UUID;

@Repository
public interface ParticipantEventRepository extends JpaRepository<ParticipantEvent, UUID> {

    @Query("SELECT pe.event FROM ParticipantEvent pe WHERE pe.participant.id = :participantId")
    List<Event> findEventsByParticipantId(@Param("participantId") UUID participantId);

    @Query("SELECT pe.participant FROM ParticipantEvent pe WHERE pe.event.id = :eventId")
    List<Participant> findParticipantsByEventId(@Param("eventId") UUID eventId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ParticipantEvent pe WHERE pe.event.id = :eventId AND pe.participant.id = :participantId")
    void deleteByEventIdAndParticipantId(@Param("eventId") UUID eventId, @Param("participantId") UUID participantId);

    boolean existsByParticipantIdAndEventId(UUID participantId, UUID eventId);

    // Compte les participants DISTINCTS (personnes uniques)
    @Query("SELECT COUNT(DISTINCT pe.participant.id) FROM ParticipantEvent pe")
    long countUniqueParticipants();

    // Compte les participants uniques pour une liste d'événements
    @Query("SELECT COUNT(DISTINCT pe.participant.id) FROM ParticipantEvent pe WHERE pe.event.id IN :eventIds")
    long countUniqueParticipantsByEventIds(@Param("eventIds") Collection<UUID> eventIds);

    // Compte le nombre total d'inscriptions pour une liste d'événements (statistiques cloisonnées par espace)
    long countByEventIdIn(Collection<UUID> eventIds);

    // Compte le nombre total d'inscriptions (toutes les lignes dans ParticipantEvent)
    long count();
}