package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantMapper;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ParticipantMapper participantMapper;

    @Override
    public ParticipantDto create(ParticipantDto dto) {
        log.info("Création du participant : {} {}", dto.getFirstName(), dto.getLastName());

        // Vérifier si l'email existe déjà
        if (participantRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un participant avec cet email existe déjà : " + dto.getEmail()
            );
        }

        Participant participant = participantMapper.toEntity(dto);
        participant.setCreatedAt(LocalDateTime.now());

        participant = participantRepository.save(participant);
        log.info("Participant créé avec succès : ID = {}", participant.getId());

        return participantMapper.toDto(participant);
    }

    @Override
    public ParticipantDto update(ParticipantDto dto) {
        log.info("Mise à jour du participant : ID = {}", dto.getId());

        return participantRepository.findById(dto.getId())
                .map(existing -> {
                    // Vérifier si le nouvel email n'est pas déjà utilisé par un autre participant
                    if (!existing.getEmail().equals(dto.getEmail())) {
                        participantRepository.findByEmail(dto.getEmail()).ifPresent(other -> {
                            if (!other.getId().equals(existing.getId())) {
                                throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Cet email est déjà utilisé par un autre participant"
                                );
                            }
                        });
                    }

                    participantMapper.updateEntityFromDto(dto, existing);
                    existing.setUpdatedAt(LocalDateTime.now());

                    return participantMapper.toDto(participantRepository.save(existing));
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Participant non trouvé avec l'ID : " + dto.getId()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> findAll() {
        log.info("Récupération de tous les participants");
        return participantMapper.toDtos(participantRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantDto findById(UUID id) {
        log.info("Recherche du participant : ID = {}", id);

        return participantRepository.findById(id)
                .map(participantMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Participant non trouvé avec l'ID : " + id
                ));
    }

    @Override
    public void delete(UUID id) {
        log.info("Suppression du participant : ID = {}", id);

        if (!participantRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Participant non trouvé avec l'ID : " + id
            );
        }

        participantRepository.deleteById(id);
        log.info("Participant supprimé avec succès");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> findByType(ParticipantType type) {
        log.info("Recherche des participants par type : {}", type);
        return participantMapper.toDtos(
                participantRepository.findByParticipantType(type)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> searchByName(String search) {
        log.info("Recherche de participants par nom : {}", search);

        if (search == null || search.trim().isEmpty()) {
            return List.of();
        }

        return participantMapper.toDtos(
                participantRepository.searchByName(search.trim())
        );
    }
}
