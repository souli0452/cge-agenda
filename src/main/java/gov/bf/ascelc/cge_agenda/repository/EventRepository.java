package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author :  <A HREF="mailto:souliissouf123@gmail.com">SOULI Issouf ()</A>
 * @version : 1.0
 * Copyright (c) 2025 ASCE-LC, All rights reserved.
 * @since : 11/12/2025 à 16:00
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = { "files" })
    Optional<Event> findWithFilesById(UUID id);
}
