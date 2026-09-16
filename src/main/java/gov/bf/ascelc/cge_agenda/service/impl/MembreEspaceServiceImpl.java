package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.MembreEspaceDto;
import gov.bf.ascelc.cge_agenda.entities.Espace;
import gov.bf.ascelc.cge_agenda.entities.MembreEspace;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceStatut;
import gov.bf.ascelc.cge_agenda.repository.EspaceRepository;
import gov.bf.ascelc.cge_agenda.repository.MembreEspaceRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.MembreEspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembreEspaceServiceImpl implements MembreEspaceService {

    private final MembreEspaceRepository membreEspaceRepository;
    private final EspaceRepository espaceRepository;
    private final EmailService emailService;
    private final SignedTokenService signedTokenService;

    @Override
    @Transactional(readOnly = true)
    public List<MembreEspaceDto> getMembres(UUID espaceId) {
        assertProprietaireOuAdmin(espaceId);
        return membreEspaceRepository.findByEspaceIdOrderByInvitedAtDesc(espaceId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MembreEspaceDto ajouterMembre(UUID espaceId, String membreEmail, String membreNom,
                                          MembreEspaceRole role, String invitedByEmail) {
        assertProprietaireOuAdmin(espaceId);
        Espace espace = espaceRepository.findById(espaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espace non trouvé : " + espaceId));

        if (membreEspaceRepository.findByEspaceIdAndMembreEmailIgnoreCase(espaceId, membreEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette personne est déjà gestionnaire de cet espace");
        }

        MembreEspace membre = membreEspaceRepository.save(MembreEspace.builder()
                .espace(espace)
                .membreEmail(membreEmail)
                .membreNom(membreNom)
                .role(role)
                .statut(MembreEspaceStatut.INVITE)
                .invitedAt(LocalDateTime.now())
                .invitedByEmail(invitedByEmail)
                .build());

        final UUID membreId = membre.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendEspaceInvitation(membreId);
            }
        });

        return toDto(membre);
    }

    @Override
    @Transactional
    public void retirerMembre(UUID espaceId, UUID membreEspaceId) {
        assertProprietaireOuAdmin(espaceId);
        MembreEspace membre = membreEspaceRepository.findById(membreEspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé"));
        if (!membre.getEspace().getId().equals(espaceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce membre n'appartient pas à cet espace");
        }
        membreEspaceRepository.deleteById(membreEspaceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RejoindreResultat rejoindre(String token) {
        var payload = signedTokenService.verify(token).orElse(null);
        if (payload == null) {
            return RejoindreResultat.INVALIDE;
        }

        MembreEspace membre = membreEspaceRepository.findById(payload.eventId()).orElse(null);
        if (membre == null || !membre.getMembreEmail().equalsIgnoreCase(payload.email())) {
            return RejoindreResultat.INVALIDE;
        }

        if (membre.getStatut() == MembreEspaceStatut.ACTIF) {
            return RejoindreResultat.DEJA_ACTIF;
        }

        membre.setStatut(MembreEspaceStatut.ACTIF);
        membre.setActivatedAt(LocalDateTime.now());
        membreEspaceRepository.save(membre);
        log.info("✓ Gestionnaire activé : {} sur l'espace {}", membre.getMembreEmail(), membre.getEspace().getId());
        return RejoindreResultat.ACTIVE;
    }

    private void assertProprietaireOuAdmin(UUID espaceId) {
        String email = currentUserEmail();
        boolean estAdmin = currentUserRoles().contains("ADMIN");
        boolean estProprietaire = espaceRepository.findById(espaceId)
                .map(e -> e.getChefEmail().equalsIgnoreCase(email))
                .orElse(false);
        if (!estAdmin && !estProprietaire) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul le propriétaire de cet espace peut gérer ses gestionnaires");
        }
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }

    private List<String> currentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().startsWith("ROLE_") ? a.getAuthority().substring(5) : a.getAuthority())
                .toList();
    }

    private MembreEspaceDto toDto(MembreEspace m) {
        return MembreEspaceDto.builder()
                .id(m.getId())
                .espaceId(m.getEspace().getId())
                .membreEmail(m.getMembreEmail())
                .membreNom(m.getMembreNom())
                .role(m.getRole())
                .statut(m.getStatut())
                .invitedAt(m.getInvitedAt())
                .activatedAt(m.getActivatedAt())
                .build();
    }
}
