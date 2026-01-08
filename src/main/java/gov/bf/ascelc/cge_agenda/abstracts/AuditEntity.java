package gov.bf.ascelc.cge_agenda.abstracts;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @CreatedDate
    @Column(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd' 'HH:mm:ss")
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(name = "updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd' 'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_id")
    private String createdById;

    @Column(name = "updated_by_id")
    private String updatedById;

    @Column(name = "current_user_first_name")
    private String currentFirstName;

    @Column(name = "current_user_last_name")
    private String currentLastName;

    @Column(name = "current_user_email")
    private String currentUserEmail;


    @PrePersist
    @PreUpdate
    public void captureUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                this.currentFirstName = jwt.getClaim("given_name");
                this.currentLastName = jwt.getClaim("family_name");
                this.currentUserEmail = jwt.getClaim("email");
            }
        } catch (Exception e) {
            // Log silencieux, ne pas bloquer la création
        }
    }
}

