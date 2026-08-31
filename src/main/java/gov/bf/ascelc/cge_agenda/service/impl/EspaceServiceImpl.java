package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.EspaceDto;
import gov.bf.ascelc.cge_agenda.entities.Espace;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceStatut;
import gov.bf.ascelc.cge_agenda.repository.EspaceRepository;
import gov.bf.ascelc.cge_agenda.repository.MembreEspaceRepository;
import gov.bf.ascelc.cge_agenda.service.EspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EspaceServiceImpl implements EspaceService {

    private final EspaceRepository espaceRepository;
    private final MembreEspaceRepository membreEspaceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EspaceDto> getAll() {
        return espaceRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public EspaceDto create(String nom, String chefEmail, String chefNom) {
        if (espaceRepository.existsByChefEmailIgnoreCase(chefEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un espace existe déjà pour ce chef : " + chefEmail);
        }
        Espace espace = Espace.builder()
                .nom(nom)
                .chefEmail(chefEmail)
                .chefNom(chefNom)
                .createdAt(LocalDateTime.now())
                .build();
        return toDto(espaceRepository.save(espace));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!espaceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Espace non trouvé : " + id);
        }
        espaceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> espacesAccessibles(String email) {
        if (email == null) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        espaceRepository.findByChefEmailIgnoreCase(email).ifPresent(e -> ids.add(e.getId()));
        membreEspaceRepository.findByMembreEmailIgnoreCaseAndStatut(email, MembreEspaceStatut.ACTIF)
                .forEach(m -> ids.add(m.getEspace().getId()));
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estProprietaire(UUID espaceId, String email) {
        if (email == null) {
            return false;
        }
        return espaceRepository.findById(espaceId)
                .map(e -> e.getChefEmail().equalsIgnoreCase(email))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean peutCreerDans(UUID espaceId, String email) {
        if (email == null) {
            return false;
        }
        if (estProprietaire(espaceId, email)) {
            return true;
        }
        return membreEspaceRepository.existsByEspaceIdAndMembreEmailIgnoreCaseAndStatut(
                espaceId, email, MembreEspaceStatut.ACTIF);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspaceDto> mesEspaces() {
        String email = currentUserEmail();
        List<UUID> ids = espacesAccessibles(email);
        if (ids.isEmpty()) {
            return List.of();
        }
        return espaceRepository.findAllById(ids).stream().map(this::toDto).toList();
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }

    private EspaceDto toDto(Espace e) {
        return EspaceDto.builder()
                .id(e.getId())
                .nom(e.getNom())
                .chefEmail(e.getChefEmail())
                .chefNom(e.getChefNom())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
