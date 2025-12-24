package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.ScheduleDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import gov.bf.ascelc.cge_agenda.mapper.ScheduleMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final EventRepository eventRepository;

    @Override
    public ScheduleDto create(ScheduleDto dto) {
        log.info("Création d'un horaire pour l'événement : {}", dto.getEventId());

        // Vérifier que l'événement existe
        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + dto.getEventId()
                ));

        // Valider que la date est dans la période de l'événement
        if (dto.getDateJour().isBefore(event.getStartDate()) ||
                dto.getDateJour().isAfter(event.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("La date %s n'est pas dans la période de l'événement (%s - %s)",
                            dto.getDateJour(), event.getStartDate(), event.getEndDate())
            );
        }

        // Valider la plage horaire
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être supérieure à l'heure de début"
            );
        }

        Schedule schedule = scheduleMapper.toEntity(dto);
        schedule.setEvent(event);
        schedule.setCreatedAt(LocalDateTime.now());

        schedule = scheduleRepository.save(schedule);
        log.info("Horaire créé avec succès : ID = {}", schedule.getId());

        return scheduleMapper.toDto(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDto> findByEventId(UUID eventId) {
        log.info("Récupération des horaires pour l'événement : {}", eventId);

        // Vérifier que l'événement existe
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Événement non trouvé avec l'ID : " + eventId
            );
        }

        List<Schedule> schedules = scheduleRepository.findByEventId(eventId);
        log.info("{} horaires trouvés", schedules.size());

        return scheduleMapper.toDtos(schedules);
    }

    @Override
    public ScheduleDto update(ScheduleDto dto) {
        log.info("Mise à jour de l'horaire : ID = {}", dto.getId());

        return scheduleRepository.findById(dto.getId())
                .map(existing -> {
                    // Vérifier que l'événement existe
                    if (dto.getEventId() != null) {
                        Event event = eventRepository.findById(dto.getEventId())
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Événement non trouvé avec l'ID : " + dto.getEventId()
                                ));
                        existing.setEvent(event);
                    }

                    // Valider la plage horaire
                    if (!dto.getEndTime().isAfter(dto.getStartTime())) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "L'heure de fin doit être supérieure à l'heure de début"
                        );
                    }

                    scheduleMapper.updateEntityFromDto(dto, existing);
                    existing.setUpdatedAt(LocalDateTime.now());

                    Schedule updated = scheduleRepository.save(existing);
                    log.info("Horaire mis à jour avec succès");

                    return scheduleMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Horaire non trouvé avec l'ID : " + dto.getId()
                ));
    }

    @Override
    public void delete(UUID id) {
        log.info("Suppression de l'horaire : ID = {}", id);

        if (!scheduleRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Horaire non trouvé avec l'ID : " + id
            );
        }

        scheduleRepository.deleteById(id);
        log.info("Horaire supprimé avec succès");
    }
}
