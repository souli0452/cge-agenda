package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.ParticipantEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ParticipantEventRepository extends JpaRepository<ParticipantEvent, UUID> {

    @Query("SELECT pe.event FROM ParticipantEvent pe WHERE pe.participant.id = :participantId")
    List<Event> findEventsByParticipantId(@Param("participantId") UUID participantId);

    @Query("SELECT pe.participant FROM ParticipantEvent pe WHERE pe.event.id = :eventId")
    List<Participant> findParticipantsByEventId(@Param("eventId") UUID eventId);

    boolean existsByParticipantIdAndEventId(UUID participantId, UUID eventId);
}