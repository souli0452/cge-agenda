package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(EVENT_ROOT_URL)
public class EventController {

    @Autowired
    private EventService eventService;


    /*--------------------------------------------------------------------------/
   /                    Méthode de création d'un evènement                     /
  /--------------------------------------------------------------------------*/
    @PostMapping(CREATE_EVENT)
    public ResponseEntity<EventDto> create(@RequestBody EventDto eventDto) {
        EventDto event = eventService.create(eventDto);
        return new ResponseEntity<>(event, HttpStatus.CREATED);
    }

    /*--------------------------------------------------------------------------/
   /                Méthode de mise à jour d'un evènement                      /
  /--------------------------------------------------------------------------*/
    @PutMapping(UPDATE_EVENT)
    public ResponseEntity<EventDto> update(@RequestBody EventDto eventDto) {
        EventDto event = eventService.update(eventDto);
        return new ResponseEntity<>(event, HttpStatus.OK);
    }

    /*--------------------------------------------------------------------------/
  /                Méthode de pour lister les évènement                       /
/--------------------------------------------------------------------------*/
    @GetMapping(GET_ALL_EVENT)
    public ResponseEntity<List<EventDto>> allEvents() {
        List<EventDto> events = eventService.allEvents();
        return new ResponseEntity<>(events, HttpStatus.OK);
    }

    /*--------------------------------------------------------------------------/
   /                Méthode de pour lister les évènement                       /
  /--------------------------------------------------------------------------*/
    @GetMapping(GET_EVENT_BY_ID)
    public ResponseEntity<EventDto> getEventById(@RequestParam UUID id) {
        EventDto event = eventService.getEventById(id);
        return new ResponseEntity<>(event, HttpStatus.OK);
    }

      /*--------------------------------------------------------------------------/
     /                    Méthode de suppression d'un evènement                  /
    /--------------------------------------------------------------------------*/

    @DeleteMapping(DELETE_EVENT)
    public void deleteyId(@PathVariable UUID id) {
        eventService.delete(id);

    }
}
