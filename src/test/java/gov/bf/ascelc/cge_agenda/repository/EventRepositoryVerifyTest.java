package gov.bf.ascelc.cge_agenda.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EventRepositoryVerifyTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void findAllWithParticipantsOrderedByStatusAndProximity_runsAgainstRealPostgres() {
        eventRepository.findAllWithParticipantsOrderedByStatusAndProximity();
    }

    @Test
    void findAllDeleted_runsAgainstRealPostgres() {
        eventRepository.findAllDeleted();
    }
}
