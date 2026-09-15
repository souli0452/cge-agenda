package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.EspaceDto;
import gov.bf.ascelc.cge_agenda.entities.Espace;
import gov.bf.ascelc.cge_agenda.repository.EspaceRepository;
import gov.bf.ascelc.cge_agenda.repository.MembreEspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EspaceServiceImplTest {

    private EspaceRepository espaceRepository;
    private EspaceServiceImpl service;

    private static final UUID ESPACE_ID = UUID.randomUUID();
    private static final UUID AUTRE_ESPACE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        espaceRepository = mock(EspaceRepository.class);
        MembreEspaceRepository membreEspaceRepository = mock(MembreEspaceRepository.class);
        service = new EspaceServiceImpl(espaceRepository, membreEspaceRepository);
    }

    private Espace espace(boolean actif) {
        return Espace.builder()
                .id(ESPACE_ID)
                .nom("Cabinet du Directeur")
                .chefEmail("chef@ascelc.bf")
                .chefNom("Jean Dupont")
                .actif(actif)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // update
    // ==========================================
    @Test
    void update_changesNomChefNomAndChefEmail() {
        Espace existing = espace(true);
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.of(existing));
        when(espaceRepository.existsByChefEmailIgnoreCaseAndIdNot("nouveau-chef@ascelc.bf", ESPACE_ID))
                .thenReturn(false);
        when(espaceRepository.save(any(Espace.class))).thenAnswer(inv -> inv.getArgument(0));

        EspaceDto result = service.update(ESPACE_ID, "Nouveau nom", "nouveau-chef@ascelc.bf", "Nouveau Chef");

        assertThat(result.getNom()).isEqualTo("Nouveau nom");
        assertThat(result.getChefEmail()).isEqualTo("nouveau-chef@ascelc.bf");
        assertThat(result.getChefNom()).isEqualTo("Nouveau Chef");
    }

    @Test
    void update_whenEspaceNotFound_throws404() {
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ESPACE_ID, "Nom", "chef@ascelc.bf", "Chef"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_whenChefEmailUsedByAnotherEspace_throws409() {
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.of(espace(true)));
        when(espaceRepository.existsByChefEmailIgnoreCaseAndIdNot("deja-pris@ascelc.bf", ESPACE_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(ESPACE_ID, "Nom", "deja-pris@ascelc.bf", "Chef"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void update_whenChefEmailUnchanged_doesNotConflictWithItself() {
        Espace existing = espace(true);
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.of(existing));
        when(espaceRepository.existsByChefEmailIgnoreCaseAndIdNot("chef@ascelc.bf", ESPACE_ID))
                .thenReturn(false);
        when(espaceRepository.save(any(Espace.class))).thenAnswer(inv -> inv.getArgument(0));

        EspaceDto result = service.update(ESPACE_ID, "Nom modifié", "chef@ascelc.bf", "Jean Dupont");

        assertThat(result.getNom()).isEqualTo("Nom modifié");
    }

    // ==========================================
    // setActif (remplace la suppression physique)
    // ==========================================
    @Test
    void setActif_false_marksEspaceInactive() {
        Espace existing = espace(true);
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.of(existing));
        when(espaceRepository.save(any(Espace.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setActif(ESPACE_ID, false);

        assertThat(existing.isActif()).isFalse();
    }

    @Test
    void setActif_whenEspaceNotFound_throws404() {
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActif(ESPACE_ID, false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ==========================================
    // peutCreerDans — un espace désactivé ne doit plus permettre de créer d'événement,
    // même pour son propriétaire (sinon "désactiver" n'a aucun effet réel).
    // ==========================================
    @Test
    void peutCreerDans_whenEspaceInactifEtProprietaire_returnsFalse() {
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.of(espace(false)));

        boolean result = service.peutCreerDans(ESPACE_ID, "chef@ascelc.bf");

        assertThat(result).isFalse();
    }

    @Test
    void peutCreerDans_whenEspaceActifEtProprietaire_returnsTrue() {
        when(espaceRepository.findById(ESPACE_ID)).thenReturn(Optional.of(espace(true)));

        boolean result = service.peutCreerDans(ESPACE_ID, "chef@ascelc.bf");

        assertThat(result).isTrue();
    }
}
