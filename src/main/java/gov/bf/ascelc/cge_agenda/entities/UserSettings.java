package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_settings")
public class UserSettings extends AuditEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "email_invitation_enabled", nullable = false)
    private boolean emailInvitationEnabled;

    @Column(name = "email_validation_enabled", nullable = false)
    private boolean emailValidationEnabled;

    @Column(name = "email_reminder_enabled", nullable = false)
    private boolean emailReminderEnabled;
}
