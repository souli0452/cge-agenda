package gov.bf.ascelc.cge_agenda.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDto {

    private UUID id;
    private String userId;
    private String userEmail;
    private boolean emailInvitationEnabled;
    private boolean emailValidationEnabled;
    private boolean emailReminderEnabled;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd' 'HH:mm:ss")
    private LocalDateTime updatedAt;
}
