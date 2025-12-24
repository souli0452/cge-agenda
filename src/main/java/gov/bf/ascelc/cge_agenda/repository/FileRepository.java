package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {

    /**
     * Find all files for a specific event
     */
    List<File> findByEventId(UUID eventId);

    /**
     * Delete all files for a specific event
     */
    void deleteByEventId(UUID eventId);
}