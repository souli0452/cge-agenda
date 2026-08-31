package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.UserSettingsDto;
import gov.bf.ascelc.cge_agenda.entities.UserSettings;
import gov.bf.ascelc.cge_agenda.repository.UserSettingsRepository;
import gov.bf.ascelc.cge_agenda.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final UserSettingsRepository userSettingsRepository;

    @Override
    @Transactional
    public UserSettingsDto getSettings() {
        UserSettings settings = findOrCreateForCurrentUser();
        return toDto(settings);
    }

    @Override
    @Transactional
    public UserSettingsDto updateSettings(UserSettingsDto dto) {
        UserSettings settings = findOrCreateForCurrentUser();
        settings.setEmailInvitationEnabled(dto.isEmailInvitationEnabled());
        settings.setEmailValidationEnabled(dto.isEmailValidationEnabled());
        settings.setEmailReminderEnabled(dto.isEmailReminderEnabled());
        return toDto(userSettingsRepository.save(settings));
    }

    private UserSettings findOrCreateForCurrentUser() {
        String userId = currentUserId();
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> userSettingsRepository.save(
                        UserSettings.builder()
                                .userId(userId)
                                .userEmail(currentUserEmail())
                                .emailInvitationEnabled(true)
                                .emailValidationEnabled(true)
                                .emailReminderEnabled(true)
                                .build()
                ));
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "anonymous";
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }

    private UserSettingsDto toDto(UserSettings s) {
        return UserSettingsDto.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .userEmail(s.getUserEmail())
                .emailInvitationEnabled(s.isEmailInvitationEnabled())
                .emailValidationEnabled(s.isEmailValidationEnabled())
                .emailReminderEnabled(s.isEmailReminderEnabled())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
