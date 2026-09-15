package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.UserSettingsDto;

public interface SettingsService {

    UserSettingsDto getSettings();

    UserSettingsDto updateSettings(UserSettingsDto dto);
}
