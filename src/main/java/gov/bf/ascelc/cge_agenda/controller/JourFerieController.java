package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.JourFerieDto;
import gov.bf.ascelc.cge_agenda.entities.JourFerie;
import gov.bf.ascelc.cge_agenda.repository.JourFerieRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * CRUD des jours fériés, exclus du calcul des échéances de validation
 * (voir BusinessHoursCalculator / EventServiceImpl.calculerEcheance).
 */
@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
public class JourFerieController {

    private final JourFerieRepository jourFerieRepository;

    @GetMapping(ADMIN_JOURS_FERIES)
    public ResponseEntity<List<JourFerieDto>> getAll() {
        return ResponseEntity.ok(jourFerieRepository.findAllByOrderByDateAsc().stream().map(this::toDto).toList());
    }

    @PostMapping(ADMIN_JOURS_FERIES)
    public ResponseEntity<JourFerieDto> create(@Valid @RequestBody JourFerieDto dto) {
        JourFerie saved = jourFerieRepository.save(JourFerie.builder()
                .date(dto.getDate())
                .libelle(dto.getLibelle())
                .build());
        return new ResponseEntity<>(toDto(saved), HttpStatus.CREATED);
    }

    @DeleteMapping(ADMIN_JOUR_FERIE_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jourFerieRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private JourFerieDto toDto(JourFerie j) {
        return JourFerieDto.builder().id(j.getId()).date(j.getDate()).libelle(j.getLibelle()).build();
    }
}
