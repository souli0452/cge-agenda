package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.OrgConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgConfigRepository extends JpaRepository<OrgConfig, UUID> {

    Optional<OrgConfig> findFirstByOrderByCreatedAtAsc();
}
