package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.SchedulerConfigDto;

public interface SchedulerConfigService {

    SchedulerConfigDto getConfig();

    SchedulerConfigDto updateConfig(SchedulerConfigDto dto);

    String runNow();
}
