package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.JourFerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JourFerieRepository extends JpaRepository<JourFerie, UUID> {
    List<JourFerie> findAllByOrderByDateAsc();
    List<JourFerie> findByDateGreaterThanEqual(LocalDate from);
}
