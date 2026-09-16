package gov.bf.ascelc.cge_agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KcRoleDto {
    private String id;
    private String name;
    private String description;
}
