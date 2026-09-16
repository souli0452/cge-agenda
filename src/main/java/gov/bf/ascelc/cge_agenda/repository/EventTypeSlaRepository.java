package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.EventTypeSla;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventTypeSlaRepository extends JpaRepository<EventTypeSla, UUID> {
    Optional<EventTypeSla> findByEventType(EventType eventType);
}
