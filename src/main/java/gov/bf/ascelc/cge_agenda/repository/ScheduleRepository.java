package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    /**
     * Récupère tous les horaires d'un événement
     */
    List<Schedule> findByEventId(UUID eventId);

    /**
     * Récupère les horaires d'un événement pour une date spécifique
     */
    List<Schedule> findByEventIdAndDateJour(UUID eventId, LocalDate dateJour);

    /**
     * Récupère les horaires dans une période
     */
    @Query("SELECT s FROM Schedule s WHERE s.event.id = :eventId " +
            "AND s.dateJour BETWEEN :startDate AND :endDate " +
            "ORDER BY s.dateJour, s.startTime")
    List<Schedule> findByEventIdAndDateRange(
            @Param("eventId") UUID eventId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Supprime tous les horaires d'un événement
     */
    void deleteByEventId(UUID eventId);
}
