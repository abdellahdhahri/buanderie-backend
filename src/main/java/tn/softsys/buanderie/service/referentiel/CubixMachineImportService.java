package tn.softsys.buanderie.service.referentiel;

import org.springframework.util.MultiValueMap;

import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.dto.MachineImportRequest;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.entity.enums.TypeMachine;
import tn.softsys.buanderie.exception.BusinessException;

/**
 * Import des machines « à la CUBIX » : on liste les immobilisations importables, on choisit laveur ou séchoir,
 * CUBIX les classe (table Autoclave), puis on relit les machines pour les enregistrer ici.
 * Disponible uniquement en mode EXTERNE avec le connecteur CUBIX ; les autres modes ne passent jamais par ici.
 */
@Service
@RequiredArgsConstructor
public class CubixMachineImportService {

    private static final String SUFFIXE_MACHINES = "/BuanderieApi/Machines";

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final SynchronisationMachinesService synchronisationMachines;

    /** Immobilisations importables (Categorie = 0, famille Linge). */
    public JsonNode importables(String q, int page, int pageSize) {
        ConfigurationIntegration config = exigerModeCubix();
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(baseApi() + "/Importables")
                .queryParam("page", page)
                .queryParam("pageSize", pageSize);
        if (q != null && !q.isBlank()) {
            url.queryParam("q", q.trim());
        }
        return connecteur.lireJson(url.build().encode().toUriString(), config);
    }

    /** Importe la sélection dans CUBIX puis relit les machines pour les enregistrer localement. */
    public Map<String, Object> importer(List<MachineImportRequest> demandes) {
        ConfigurationIntegration config = exigerModeCubix();

        if (demandes == null || demandes.isEmpty()) {
            throw new BusinessException("Sélectionnez au moins une machine à importer.");
        }

        List<Long> laveurs = new ArrayList<>();
        List<Long> secheurs = new ArrayList<>();
        for (MachineImportRequest d : demandes) {
            if (d == null || d.getId() == null) {
                throw new BusinessException("Chaque machine doit avoir un identifiant d'immobilisation.");
            }
            if (d.getType() == TypeMachine.LAVEUR) {
                laveurs.add(d.getId());
            } else if (d.getType() == TypeMachine.SECHEUR) {
                secheurs.add(d.getId());
            } else {
                throw new BusinessException("Le type de la machine " + d.getId()
                        + " doit être LAVEUR ou SECHEUR (CUBIX ne gère pas d'autre type).");
            }
        }

        List<Object> importes = new ArrayList<>();
        List<Object> ignores = new ArrayList<>();
        if (!laveurs.isEmpty()) {
            appelerImport(config, "laveur", laveurs, importes, ignores);
        }
        if (!secheurs.isEmpty()) {
            appelerImport(config, "sechoir", secheurs, importes, ignores);
        }

        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("importes", importes);
        resultat.put("ignores", ignores);

        // Toujours relire les machines de CUBIX : une machine déjà importée (deja_importe) doit aussi arriver ici.
        JournalSynchronisation journal = synchronisationMachines.synchroniser("IMPORT_CUBIX");
        resultat.put("synchronisation", journal.getStatut());
        resultat.put("message", journal.getMessage());
        return resultat;
    }

    private void appelerImport(ConfigurationIntegration config, String type, List<Long> ids,
                               List<Object> importes, List<Object> ignores) {
        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("type", type);
        ids.forEach(id -> champs.add("ids", String.valueOf(id)));

        JsonNode reponse = connecteur.ecrireFormulaire(baseApi() + "/ImporterMachines", config, champs);
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

    /** URL de l'API CUBIX, déduite de l'URL configurée pour les machines (sans le « /Machines » final). */
    String baseApi() {
        String url = integrationService.urlComplete(Referentiel.MACHINE);
        int fin = url.indexOf('?');
        String sansParametres = fin >= 0 ? url.substring(0, fin) : url;
        if (!sansParametres.toLowerCase().endsWith(SUFFIXE_MACHINES.toLowerCase())) {
            throw new BusinessException("L'URL configurée pour les machines (" + sansParametres + ") n'est pas celle de la "
                    + "nouvelle API CUBIX : elle doit se terminer par « " + SUFFIXE_MACHINES
                    + " ». Réappliquez le modèle CUBIX (POST /api/integration/modele/CUBIX).");
        }
        return sansParametres.substring(0, sansParametres.length() - "/Machines".length());
    }
}
