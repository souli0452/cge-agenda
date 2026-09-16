package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ParticipantService {
    ParticipantDto create(ParticipantDto dto);
    ParticipantDto update(ParticipantDto dto);
    List<ParticipantDto> findAll();
    ParticipantDto findById(UUID id);
    void delete(UUID id);
    List<ParticipantDto> findByType(ParticipantType type);
    List<ParticipantDto> searchByName(String search);

    Page<ParticipantDto> findPaged(String keyword, ParticipantType type, Pageable pageable);
    List<ParticipantDto> getCorbeille();
    ParticipantDto restore(UUID id);
    void deletePermanently(UUID id);
}
