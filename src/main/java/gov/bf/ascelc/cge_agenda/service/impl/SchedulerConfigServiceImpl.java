package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.SchedulerConfigDto;
import gov.bf.ascelc.cge_agenda.entities.SchedulerConfig;
import gov.bf.ascelc.cge_agenda.repository.SchedulerConfigRepository;
import gov.bf.ascelc.cge_agenda.scheduler.ReminderScheduler;
import gov.bf.ascelc.cge_agenda.service.SchedulerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerConfigServiceImpl implements SchedulerConfigService {

    private final SchedulerConfigRepository schedulerConfigRepository;
    private final ReminderScheduler reminderScheduler;

    @Override
    @Transactional
    public SchedulerConfigDto getConfig() {
        return toDto(findOrCreateConfig());
    }

    @Override
    @Transactional
    public SchedulerConfigDto updateConfig(SchedulerConfigDto dto) {
        SchedulerConfig config = findOrCreateConfig();
        config.setReminderEnabled(dto.isReminderEnabled());
        config.setSendHour(dto.getSendHour());
        config.setReminderDays(dto.getReminderDays() != null ? new ArrayList<>(dto.getReminderDays()) : new ArrayList<>());
        return toDto(schedulerConfigRepository.save(config));
    }

    @Override
    public String runNow() {
        return reminderScheduler.runNow();
    }

    private SchedulerConfig findOrCreateConfig() {
        return schedulerConfigRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> schedulerConfigRepository.save(
                        SchedulerConfig.builder()
                                .reminderEnabled(true)
                                .sendHour(7)
                                .reminderDays(new ArrayList<>(List.of(7, 1)))
                                .build()
                ));
    }

    private LocalDateTime nextRun(SchedulerConfig config) {
        LocalDateTime candidate = LocalDate.now().atTime(config.getSendHour(), 0);
        if (candidate.isBefore(LocalDateTime.now())) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }

    private SchedulerConfigDto toDto(SchedulerConfig c) {
        return SchedulerConfigDto.builder()
                .reminderEnabled(c.isReminderEnabled())
                .sendHour(c.getSendHour())
                .reminderDays(c.getReminderDays())
                .nextScheduledRun(nextRun(c))
                .updatedAt(c.getUpdatedAt())
                .updatedBy(c.getCurrentUserEmail() != null ? c.getCurrentUserEmail() : currentUserEmail())
                .build();
    }
}
