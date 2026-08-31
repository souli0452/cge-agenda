package gov.bf.ascelc.cge_agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPayloadDto {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean enabled;
    private String password;
    /** Si vrai, l'utilisateur devra configurer la double authentification (TOTP) à sa prochaine connexion. */
    private boolean requireMfa;
}
