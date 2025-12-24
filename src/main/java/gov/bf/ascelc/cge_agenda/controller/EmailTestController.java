package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/send-reminder")
    public String testReminder() {
        Event event = eventRepository.findAll().get(0);
        emailService.sendEventReminder(event, 7);
        return "Email envoyé !";
    }
}
