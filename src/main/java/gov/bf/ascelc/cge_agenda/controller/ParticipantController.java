package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import gov.bf.ascelc.cge_agenda.service.ParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(PARTICIPANT_ROOT_URL)
@RequiredArgsConstructor

public class ParticipantController {

    private final ParticipantService participantService;

    /**
     * Créer un nouveau participant
     */
    @PostMapping(CREATE_PARTICIPANT)
    public ResponseEntity<ParticipantDto> create(@Valid @RequestBody ParticipantDto participantDto) {
        ParticipantDto created = participantService.create(participantDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Mettre à jour un participant existant
     */
    @PutMapping(UPDATE_PARTICIPANT)
    public ResponseEntity<ParticipantDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody ParticipantDto participantDto
    ) {
        participantDto.setId(id);
        ParticipantDto updated = participantService.update(participantDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupérer tous les participants
     */
    @GetMapping(GET_ALL_PARTICIPANT)
    public ResponseEntity<List<ParticipantDto>> findAll() {
        List<ParticipantDto> participants = participantService.findAll();
        return ResponseEntity.ok(participants);
    }

    /**
     * Récupérer un participant par son ID
     */
    @GetMapping(GET_PARTICIPANT_BY_ID)
    public ResponseEntity<ParticipantDto> findById(@PathVariable UUID id) {
        ParticipantDto participant = participantService.findById(id);
        return ResponseEntity.ok(participant);
    }

    /**
     * Supprimer un participant
     */
    @DeleteMapping(DELETE_PARTICIPANT)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Rechercher des participants par type
     */
    @GetMapping(GET_PARTICIPANTS_BY_TYPE)
    public ResponseEntity<List<ParticipantDto>> findByType(@PathVariable ParticipantType type) {
        List<ParticipantDto> participants = participantService.findByType(type);
        return ResponseEntity.ok(participants);
    }

    /**
     * Rechercher des participants par nom/prénom
     */
    @GetMapping(SEARCH_PARTICIPANTS)
    public ResponseEntity<List<ParticipantDto>> searchByName(@RequestParam String query) {
        List<ParticipantDto> participants = participantService.searchByName(query);
        return ResponseEntity.ok(participants);
    }

    /**
     * Autocomplétion des participants par nom/prénom (saisie assistée)
     */
    @GetMapping(AUTOCOMPLETE_PARTICIPANTS)
    public ResponseEntity<List<ParticipantDto>> autocomplete(@RequestParam String q) {
        List<ParticipantDto> participants = participantService.searchByName(q);
        return ResponseEntity.ok(participants);
    }

    /**
     * Liste paginée, filtrable et triable des participants (hors corbeille)
     */
    @GetMapping(GET_PARTICIPANTS_PAGED)
    public ResponseEntity<PagedModel<ParticipantDto>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ParticipantType type
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Page<ParticipantDto> result = participantService.findPaged(
                keyword, type, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(new PagedModel<>(result));
    }

    /**
     * Participants dans la corbeille
     */
    @GetMapping(GET_PARTICIPANT_CORBEILLE)
    public ResponseEntity<List<ParticipantDto>> getCorbeille() {
        return ResponseEntity.ok(participantService.getCorbeille());
    }

    /**
     * Restaurer un participant depuis la corbeille
     */
    @PutMapping(RESTORE_PARTICIPANT)
    public ResponseEntity<ParticipantDto> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(participantService.restore(id));
    }

    /**
     * Supprimer définitivement un participant
     */
    @DeleteMapping(DELETE_PARTICIPANT_PERMANENT)
    public ResponseEntity<Void> deletePermanently(@PathVariable UUID id) {
        participantService.deletePermanently(id);
        return ResponseEntity.noContent().build();
    }
}
