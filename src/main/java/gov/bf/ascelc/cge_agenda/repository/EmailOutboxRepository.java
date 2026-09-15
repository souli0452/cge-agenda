package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.EmailOutbox;
import gov.bf.ascelc.cge_agenda.enums.EmailOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    List<EmailOutbox> findByStatusAndNextAttemptAtLessThanEqual(
            EmailOutboxStatus status, LocalDateTime now);

    long countByStatus(EmailOutboxStatus status);
}
