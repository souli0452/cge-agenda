package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = { "files" })
    Optional<Event> findWithFilesById(UUID id);

    long count();

    @Query("SELECT e.status, COUNT(e) FROM Event e GROUP BY e.status")
    List<Object[]> countByStatus();

    @Query("SELECT e.type, COUNT(e) FROM Event e GROUP BY e.type")
    List<Object[]> countByType();

    @Query("SELECT e FROM Event e WHERE e.startDate > :today AND e.startDate < :weekFromNow ORDER BY e.startDate ASC")
    List<Event> findUpcomingEvents(@Param("today") LocalDate today, @Param("weekFromNow") LocalDate weekFromNow);

    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :startOfMonth AND :endOfMonth ORDER BY e.startDate DESC")
    List<Event> findByMonth(@Param("startOfMonth") LocalDate startOfMonth, @Param("endOfMonth") LocalDate endOfMonth);

    @Query("SELECT e FROM Event e WHERE YEAR(e.startDate) = :year ORDER BY e.startDate DESC")
    List<Event> findByYear(@Param("year") int year);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.participantEvents pe " +
            "LEFT JOIN FETCH pe.participant " +
            "WHERE e.deleted = false " +
            "ORDER BY " +
            "CASE e.status WHEN 'EN_COURS' THEN 1 WHEN 'PLANIFIE' THEN 2 WHEN 'REPORTER' THEN 3 WHEN 'TERMINE' THEN 4 WHEN 'ANNULE' THEN 5 ELSE 6 END ASC, " +
            "CASE WHEN e.startDate >= CURRENT_DATE THEN 0 ELSE 1 END ASC, " +
            "CASE WHEN e.startDate >= CURRENT_DATE THEN e.startDate END ASC, " +
            "e.startDate DESC")
    List<Event> findAllWithParticipantsOrderedByStatusAndProximity();

    @Query("SELECT e FROM Event e WHERE e.deleted = true ORDER BY e.updatedAt DESC")
    List<Event> findAllDeleted();

    @Query("SELECT e FROM Event e WHERE " +
            "(:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:type IS NULL OR e.type = :type) AND " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:startDate IS NULL OR :endDate IS NULL OR (e.endDate >= :startDate AND e.startDate <= :endDate)) " +
            "ORDER BY " +
            "CASE e.status " +
            "   WHEN 'EN_COURS' THEN 1 " +
            "   WHEN 'PLANIFIE' THEN 2 " +
            "   WHEN 'REPORTER' THEN 3 " +
            "   WHEN 'TERMINE' THEN 4 " +
            "   WHEN 'ANNULE' THEN 5 " +
            "   ELSE 6 " +
            "END ASC, " +
            "CASE WHEN e.startDate >= CURRENT_DATE THEN 0 ELSE 1 END ASC, " +
            "CASE WHEN e.startDate >= CURRENT_DATE THEN e.startDate END ASC, " +
            "e.startDate DESC")
    List<Event> search(
            @Param("keyword") String keyword,
            @Param("type") EventType type,
            @Param("status") EventStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT pe.event FROM ParticipantEvent pe WHERE pe.participant.id = :participantId")
    List<Event> findEventsByParticipantId(@Param("participantId") UUID participantId);

    List<Event> findByStartDateAndStatus(LocalDate startDate, EventStatus status);
}