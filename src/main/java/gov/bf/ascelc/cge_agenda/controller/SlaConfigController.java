package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.EventTypeSlaDto;
import gov.bf.ascelc.cge_agenda.entities.EventTypeSla;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.repository.EventTypeSlaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * Configuration du délai de validation (SLA) par type d'événement, utilisée pour
 * calculer l'échéance de validation à la soumission (EventServiceImpl.calculerEcheance).
 */
@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
public class SlaConfigController {

    private final EventTypeSlaRepository eventTypeSlaRepository;

    @GetMapping(ADMIN_SLA)
    public ResponseEntity<List<EventTypeSlaDto>> getAll() {
        return ResponseEntity.ok(eventTypeSlaRepository.findAll().stream().map(this::toDto).toList());
    }

    @PutMapping(ADMIN_SLA_BY_TYPE)
    public ResponseEntity<EventTypeSlaDto> update(
            @PathVariable EventType eventType,
            @RequestBody EventTypeSlaDto dto
    ) {
        if (dto.getDelaiHeuresOuvrables() == null || dto.getDelaiHeuresOuvrables() <= 0
                || dto.getDelaiAvantEvenementHeures() == null || dto.getDelaiAvantEvenementHeures() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les délais doivent être des entiers positifs");
        }
        EventTypeSla sla = eventTypeSlaRepository.findByEventType(eventType)
                .orElseGet(() -> EventTypeSla.builder().eventType(eventType).build());
        sla.setDelaiHeuresOuvrables(dto.getDelaiHeuresOuvrables());
        sla.setDelaiAvantEvenementHeures(dto.getDelaiAvantEvenementHeures());
        return ResponseEntity.ok(toDto(eventTypeSlaRepository.save(sla)));
    }

    private EventTypeSlaDto toDto(EventTypeSla s) {
        return EventTypeSlaDto.builder()
                .eventType(s.getEventType())
                .delaiHeuresOuvrables(s.getDelaiHeuresOuvrables())
                .delaiAvantEvenementHeures(s.getDelaiAvantEvenementHeures())
                .build();
    }
}
