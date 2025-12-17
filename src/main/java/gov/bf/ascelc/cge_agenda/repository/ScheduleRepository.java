package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
}
