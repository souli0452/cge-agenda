package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.MembreEspace;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembreEspaceRepository extends JpaRepository<MembreEspace, UUID> {

    List<MembreEspace> findByEspaceIdOrderByInvitedAtDesc(UUID espaceId);

    List<MembreEspace> findByMembreEmailIgnoreCaseAndStatut(String membreEmail, MembreEspaceStatut statut);

    Optional<MembreEspace> findByEspaceIdAndMembreEmailIgnoreCase(UUID espaceId, String membreEmail);

    boolean existsByEspaceIdAndMembreEmailIgnoreCaseAndStatut(UUID espaceId, String membreEmail, MembreEspaceStatut statut);

    List<MembreEspace> findByEspaceIdAndRoleAndStatut(UUID espaceId, MembreEspaceRole role, MembreEspaceStatut statut);
}
