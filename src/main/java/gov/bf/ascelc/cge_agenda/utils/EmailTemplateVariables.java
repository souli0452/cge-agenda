package gov.bf.ascelc.cge_agenda.utils;

import java.util.Map;

/**
 * Substitution de variables `{cle}` dans les objets/corps d'emails configurables par l'admin
 * (OrgConfig). Utilisée à la fois pour l'envoi réel (EmailServiceImpl) et l'aperçu
 * (OrgConfigServiceImpl) afin que ce que l'admin voit corresponde exactement à ce qui est envoyé.
 */
public class EmailTemplateVariables {

    private EmailTemplateVariables() {
    }

    public static String substitute(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        // Variable non applicable à ce type d'email (ex. {participant} dans "Délégation") :
        // jamais exposer la syntaxe {xxx} brute au destinataire.
        return result.replaceAll("\\{[a-zA-Z_]+\\}", "");
    }
}
