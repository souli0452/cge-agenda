package gov.bf.ascelc.cge_agenda.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateVariablesTest {

    @Test
    void substitute_replacesKnownVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("evenement", "Réunion budget");
        vars.put("participant", "Jean Dupont");

        String result = EmailTemplateVariables.substitute(
                "Bonjour {participant}, l'événement {evenement} vous concerne.", vars);

        assertThat(result).isEqualTo("Bonjour Jean Dupont, l'événement Réunion budget vous concerne.");
    }

    @Test
    void substitute_nullValueBecomesEmptyString() {
        Map<String, String> vars = new HashMap<>();
        vars.put("lieu", null);

        String result = EmailTemplateVariables.substitute("Lieu : {lieu}.", vars);

        assertThat(result).isEqualTo("Lieu : .");
    }

    @Test
    void substitute_unknownVariableIsStrippedNotLeftLiteral() {
        Map<String, String> vars = new HashMap<>();
        vars.put("evenement", "Séminaire");

        String result = EmailTemplateVariables.substitute(
                "Concerne {participant} pour {evenement}.", vars);

        assertThat(result).isEqualTo("Concerne  pour Séminaire.");
    }

    @Test
    void substitute_nullTemplateReturnsEmptyString() {
        assertThat(EmailTemplateVariables.substitute(null, new HashMap<>())).isEqualTo("");
    }
}
