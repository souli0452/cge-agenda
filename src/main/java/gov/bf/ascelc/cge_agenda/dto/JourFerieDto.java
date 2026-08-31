package gov.bf.ascelc.cge_agenda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourFerieDto {
    private UUID id;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    private String libelle;
}
