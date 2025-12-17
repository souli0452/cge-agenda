package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;


@Service
@Transactional
@RequiredArgsConstructor

public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Override
    public EventDto create(EventDto dto) {
        Event event = eventMapper.toEntity(dto);
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
    }

    @Override
    public EventDto update(EventDto eventDto) {
        return eventRepository.findById(eventDto.getId()).map(eventExisted -> {
            eventMapper.updateEntityFromDto(eventDto, eventExisted);
            return eventMapper.toDto(eventRepository.save(eventExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun evènement trouvé."));
    }

    @Override
    public List<EventDto> allEvents() {
        return  eventMapper.toDtos(eventRepository.findAll()) ;
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
//        if (eventRepository.existsById(id)) {
//            return eventMapper.toDto(eventRepository.getReferenceById(id));
//        } else {
//            throw new ResponseStatusException(HttpStatus.OK, "Cet evènement n'existe pas.");
//
//        }
        return eventRepository.findById(id)
                .map(eventMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id
                ));
    }

    @Override
    public void delete(UUID id) {
            Event event=eventRepository.getReferenceById(id);
            eventRepository.delete(event);
        }


}
