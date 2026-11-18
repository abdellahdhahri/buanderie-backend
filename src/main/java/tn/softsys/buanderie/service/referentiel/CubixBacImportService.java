package tn.softsys.buanderie.service.referentiel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;


@Service
@RequiredArgsConstructor
public class CubixBacImportService {

    private static final String SUFFIXE = "/BuanderieApi/Bacs";

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final CubixMachineImportService importables;
    private final SynchronisationBacsService synchronisationBacs;

    public JsonNode importables(String q, int page, int pageSize) {
        return importables.importables(q, page, pageSize);
    }

    public Map<String, Object> importer(List<Long> ids) {
        ConfigurationIntegration config = exigerModeCubix();

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("Sélectionnez au moins un bac à importer.");
        }

        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        ids.stream().filter(i -> i != null).distinct().forEach(i -> champs.add("ids", String.valueOf(i)));

        JsonNode reponse = connecteur.ecrireFormulaire(baseApi() + "/ImporterBacs", config, champs);

        List<Object> importes = new ArrayList<>();
        List<Object> ignores = new ArrayList<>();
        JsonNode ok = reponse.get("importes");
        if (ok != null && ok.isArray()) {
            ok.forEach(n -> importes.add(n.asLong()));
        }
        JsonNode ko = reponse.get("ignores");
        if (ko != null && ko.isArray()) {
            ko.forEach(n -> {
                Map<String, Object> ligne = new LinkedHashMap<>();
                ligne.put("id", n.path("id").asLong());
                ligne.put("raison", n.path("raison").asText());
                ignores.add(ligne);
            });
        }

        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("importes", importes);
        resultat.put("ignores", ignores);

        JournalSynchronisation journal = synchronisationBacs.synchroniser("IMPORT_CUBIX");
        resultat.put("synchronisation", journal.getStatut());
        resultat.put("message", journal.getMessage());
        return resultat;
    }

    private ConfigurationIntegration exigerModeCubix() {
        ConfigurationIntegration config = integrationService.getConfiguration();
        if (config.getMode() != ModeSource.EXTERNE
                || config.getTypeConnecteur() != TypeConnecteur.CUBIX
                || config.getTypeAuthentification() != TypeAuthentification.CUBIX) {
            throw new BusinessException(
                    "L'import depuis CUBIX n'est disponible qu'en mode externe, connecteur CUBIX, authentification CUBIX.");
        }
        return config;
    }

    private String baseApi() {
        String url = integrationService.urlComplete(Referentiel.BAC);
        int fin = url.indexOf('?');
        String sansParametres = fin >= 0 ? url.substring(0, fin) : url;
        if (!sansParametres.toLowerCase().endsWith(SUFFIXE.toLowerCase())) {
            throw new BusinessException("L'URL configurée pour les bacs (" + sansParametres + ") n'est pas celle de la "
                    + "nouvelle API CUBIX : elle doit se terminer par « " + SUFFIXE
                    + " ». Réappliquez le modèle CUBIX (POST /api/integration/modele/CUBIX).");
        }
        return sansParametres.substring(0, sansParametres.length() - "/Bacs".length());
    }
}
