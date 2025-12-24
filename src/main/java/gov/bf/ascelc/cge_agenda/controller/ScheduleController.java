package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.ScheduleDto;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(SCHEDULE_ROOT_URL)
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Créer un nouvel horaire
     */
    @PostMapping(CREATE_SCHEDULE)
    public ResponseEntity<ScheduleDto> create(@Valid @RequestBody ScheduleDto scheduleDto) {
        ScheduleDto created = scheduleService.create(scheduleDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Mettre à jour un horaire
     */
    @PutMapping(UPDATE_SCHEDULE)
    public ResponseEntity<ScheduleDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleDto scheduleDto
    ) {
        scheduleDto.setId(id);
        ScheduleDto updated = scheduleService.update(scheduleDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupérer tous les horaires d'un événement
     */
    @GetMapping(GET_SCHEDULES_BY_EVENT)
    public ResponseEntity<List<ScheduleDto>> findByEventId(@PathVariable UUID eventId) {
        List<ScheduleDto> schedules = scheduleService.findByEventId(eventId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * Supprimer un horaire
     */
    @DeleteMapping(DELETE_SCHEDULE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
