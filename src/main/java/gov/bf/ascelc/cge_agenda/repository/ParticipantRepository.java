package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
     * Recherche des participants par type
     */
    List<Participant> findByParticipantType(ParticipantType type);

    /**
     * Recherche par nom ou prénom
     */
    @Query("SELECT p FROM Participant p WHERE " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Participant> searchByName(String search);

}
