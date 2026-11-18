package tn.softsys.buanderie.service.referentiel;

import tn.softsys.buanderie.entity.enums.CodeEtape;

import tn.softsys.buanderie.exception.BusinessException;

import tn.softsys.buanderie.security.CubixTokenStore;

import tn.softsys.buanderie.entity.Personnel;

import tn.softsys.buanderie.entity.Linge;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;


@Service
@RequiredArgsConstructor
public class CubixMachineEtatService {

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final CubixMachineImportService importService;
    private final CubixClient cubixClient;
    private final CubixTokenStore cubixTokens;

    private static final int CATEGORIE_LAVAGE = 3;
    private static final int CATEGORIE_SECHAGE = 4;
    private static final String MOTIF = "Synchronisation Buanderie";

    /**
     * Début d'un lavage : ValiderEtape(Lavage) fait passer les linges à l'étape Lavage dans CUBIX ET démarre le laveur.
     * Sans linge importé de CUBIX, on retombe sur DemarrerMachine seul. Bloquant.
     */
    public void demarrerLavage(Machine machine, List<Linge> linges, Integer dureeMinutes, Personnel agent) {
        validerEtape(CATEGORIE_LAVAGE, machine, linges, dureeMinutes, agent);
    }

    /** Début d'un séchage : ValiderEtape(Séchage) arrête le laveur d'origine et démarre le séchoir. Bloquant. */
    public void demarrerSechage(Machine sechoir, List<Linge> linges, Integer dureeMinutes, Personnel agent) {
        validerEtape(CATEGORIE_SECHAGE, sechoir, linges, dureeMinutes, agent);
    }

    private void validerEtape(int categorie, Machine machine, List<Linge> linges, Integer dureeMinutes, Personnel agent) {
        ConfigurationIntegration config = configurationSiActif(machine);
        if (config == null) {
            return;
        }
        List<Linge> importes = linges == null ? List.of() : linges.stream()
                                                            .filter(l -> l.getSource() == SourceDonnee.EXTERNE && l.getIdExterne() != null)
                                                            .toList();
        if (importes.isEmpty()) {
            demarrer(machine, dureeMinutes);   // aucun linge CUBIX : seule la machine est démarrée
            return;
        }

        Long etapeId = cubixClient.etapes(cubixTokens.jetonCourant()).stream()
                .filter(e -> e.categorie() == categorie)
                .map(CubixClient.Etape::id)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "CUBIX : aucune étape de catégorie " + categorie + " n'est configurée."));

        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("etapeId", String.valueOf(etapeId));
        champs.add("forcer", "true");
        champs.add("motif", MOTIF);
        champs.add("controlerHabilitation", "false");   // l'habilitation est contrôlée (avertissement) dans notre application
        if (dureeMinutes != null && dureeMinutes > 0) {
            champs.add("duree", String.valueOf(dureeMinutes));
        }
        if (agent != null && agent.getSource() == SourceDonnee.EXTERNE && agent.getIdExterne() != null) {
            champs.add("personnelId", agent.getIdExterne());
        }
        String cle = categorie == CATEGORIE_LAVAGE ? "LaveurId" : "SechoirId";
        for (int i = 0; i < importes.size(); i++) {
            champs.add("lignes[" + i + "].LingeId", importes.get(i).getIdExterne());
            champs.add("lignes[" + i + "]." + cle, machine.getIdExterne());
        }
        connecteur.ecrireFormulaire(importService.baseApi() + "/ValiderEtape", config, champs);
    }

    public void demarrer(Machine machine, Integer dureeMinutes) {
        ConfigurationIntegration config = configurationSiActif(machine);
        if (config == null) {
            return;
        }
        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("id", machine.getIdExterne());
        if (dureeMinutes != null && dureeMinutes > 0) {
            champs.add("duree", String.valueOf(dureeMinutes));
        }
        connecteur.ecrireFormulaire(importService.baseApi() + "/DemarrerMachine", config, champs);
    }

    public void arreter(Machine machine) {
        ConfigurationIntegration config = configurationSiActif(machine);
        if (config == null) {
            return;
        }
        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("id", machine.getIdExterne());
        connecteur.ecrireFormulaire(importService.baseApi() + "/ArreterMachine", config, champs);
    }

    /** La configuration si l'appel à CUBIX s'applique à cette machine, sinon null (machine ou mode interne). */
    private ConfigurationIntegration configurationSiActif(Machine machine) {
        if (machine == null || machine.getSource() != SourceDonnee.EXTERNE || machine.getIdExterne() == null) {
            return null;
        }
        ConfigurationIntegration config = integrationService.getConfiguration();
        if (config.getMode() != ModeSource.EXTERNE
                || config.getTypeConnecteur() != TypeConnecteur.CUBIX
                || config.getTypeAuthentification() != TypeAuthentification.CUBIX) {
            return null;
        }
        return config;
    }
}
