package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingService {

 private final HistoriqueLingeRepository historiqueLingeRepository;
 private final  LingeRepository lingeRepository;



    private String labelEtat(EtatBac etat) {
        if (etat == null) return "";
        return switch (etat) {
            case EN_ATTENTE_COLLECTE -> "En attente de collecte";
            case COLLECTE -> "Collecté";
            case EN_LAVAGE -> "En lavage";
            case EN_SECHAGE -> "En séchage";
            case EN_PLIAGE -> "En pliage";
            case EN_COLLAGE -> "En collage";
            case EN_STOCKAGE -> "En stockage";
            case LIVRE -> "Livré";
            case HORS_SERVICE -> "Hors service";
            case VIDE -> "Vide";
            case EN_TRI -> "bac en tri";
            case EN_RECEPTION -> "bac en reception";
            case EN_ATTENTE_SECHAGE -> "bac en attentede sechage ";
            
        };
    }
    public List<HistoriqueLinge> getHistoriqueParCodeBarre(
            String codeBarre) {

        Linge linge = lingeRepository
                .findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                codeBarre));

        return historiqueLingeRepository
                .findByLingeIdOrderByDateOperationDesc(
                        linge.getId());
    }
    private String labelOperation(TypeOperation op) {
        if (op == null) return "";
        return switch (op) {
            case RECEPTION_BAC -> "Réception du bac";
            case DEMARRAGE_LAVAGE -> "Démarrage du lavage";
            case FIN_LAVAGE -> "Fin du lavage";
            case DEMARRAGE_SECHAGE -> "Démarrage du séchage";
            case FIN_SECHAGE -> "Fin du séchage";
            case PLIAGE -> "Pliage";
            case COLLAGE -> "Collage / Étiquetage";
            case MISE_EN_STOCKAGE -> "Mise en stockage";
            case LIVRAISON -> "Livraison chambre";
            case CREATION_RECLAMATION -> "Réclamation créée";
            case RESOLUTION_RECLAMATION -> "Réclamation résolue";
            case CHANGEMENT_ETAT_BAC -> "Changement d'état";
            case CHANGEMENT_ETAT_LINGE -> "Changement état linge";
            case COLLECTE -> "Changement état linge";
            case TRI -> "Changement état linge";
            case DECLARATION_SAC_INFECTE -> "declaration sac infecte";
            case LAVAGE_SAC_INFECTE -> "lavage sac infecte";
            case RECEPTION_SAC_INFECTE -> "reception sac infecte";
            case VERIFICATION_SAC_INFECTE_ECART -> "verification ecart";
            case VERIFICATION_SAC_INFECTE_OK -> "OK";


        };

    }
    }
