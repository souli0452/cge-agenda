package gov.bf.ascelc.cge_agenda.abstracts;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
//@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy' 'HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy' 'HH:mm:ss")
    private LocalDateTime UpdateAt;

    @Column(name = "created_by_id")
    private String createdById;

    @Column(name = "update_by_id")
    private String updateById;

    @Column(name = "current_user_firts_name")
    private String currenFirstName;

    @Column(name = "current_user_last_name")
    private String currenLastName;

    @Column(name = "current_user_email")
    private String currentUserEmail;
}

