package tn.softsys.buanderie.service.referentiel;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.security.CubixTokenStore;


@Service
@RequiredArgsConstructor
public class CubixAffectationService {

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final CubixMachineImportService importService;
    private final CubixClient cubixClient;
    private final CubixTokenStore cubixTokens;

    public void affecter(Personnel personnel, LingeEtape etape) {
        ConfigurationIntegration config = integrationService.getConfiguration();
        if (config.getMode() != ModeSource.EXTERNE
                || config.getTypeConnecteur() != TypeConnecteur.CUBIX
                || config.getTypeAuthentification() != TypeAuthentification.CUBIX) {
            return;
        }
        if (personnel == null || personnel.getSource() != SourceDonnee.EXTERNE || personnel.getIdExterne() == null) {
            return;   // personnel interne : pas de compte CUBIX
        }
        Integer categorie = categorieCubix(etape == null ? null : etape.getCode());
        if (categorie == null) {
            return;   // étape sans équivalent dans CUBIX (ex. réception)
        }

        Long etapeCubixId = cubixClient.etapes(cubixTokens.jetonCourant()).stream()
                .filter(e -> e.categorie() == categorie)
                .map(CubixClient.Etape::id)
                .findFirst()
                .orElse(null);
        if (etapeCubixId == null) {
            return;   // CUBIX n'a pas cette étape configurée
        }

        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("personnelId", personnel.getIdExterne());
        champs.add("etapeId", String.valueOf(etapeCubixId));
        try {
            connecteur.ecrireFormulaire(importService.baseApi() + "/AffecterPersonnelEtape", config, champs);
        } catch (BusinessException e) {
            String m = e.getMessage() == null ? "" : e.getMessage();
            if (m.contains("association_existante")) {
                return;   // déjà affecté dans CUBIX : rien à faire
            }
            throw e;
        }
    }

    /** Catégories d'étape CUBIX : 0 collage, 2 tri, 3 lavage, 4 séchage, 5 pliage, 6 stockage, 7 livraison. */
    private Integer categorieCubix(CodeEtape code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case COLLECTE -> 0;
            case TRI -> 2;
            case LAVAGE -> 3;
            case SECHAGE -> 4;
            case PLIAGE -> 5;
            case STOCKAGE -> 6;
            case DISTRIBUTION -> 7;
            default -> null;
        };
    }
}
