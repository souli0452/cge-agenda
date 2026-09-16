package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    /**
     * Recherche un participant par email
     */
    Optional<Participant> findByEmail(String email);

    /**
     * Recherche des participants par type (hors corbeille)
     */
    List<Participant> findByParticipantTypeAndDeletedFalse(ParticipantType type);

    /**
     * Recherche par nom ou prénom (hors corbeille)
     */
    @Query("SELECT p FROM Participant p WHERE p.deleted = false AND (" +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Participant> searchByName(String search);

    List<Participant> findByDeletedFalse();

    @Query("SELECT p FROM Participant p WHERE p.deleted = true ORDER BY p.deletedAt DESC")
    List<Participant> findAllDeleted();

    @Query("SELECT p FROM Participant p WHERE p.deleted = false AND " +
            "(:keyword IS NULL OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "OR LOWER(p.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
            "(:type IS NULL OR p.participantType = :type)")
    Page<Participant> findFiltered(
            @Param("keyword") String keyword,
            @Param("type") ParticipantType type,
            Pageable pageable
    );
}
