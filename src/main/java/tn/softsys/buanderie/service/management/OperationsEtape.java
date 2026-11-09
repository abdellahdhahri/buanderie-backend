package tn.softsys.buanderie.service.management;

import java.util.EnumMap;
import java.util.Map;

import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.TypeOperation;

public final class OperationsEtape {

    private static final Map<TypeOperation, CodeEtape> ETAPES = new EnumMap<>(TypeOperation.class);
    private static final Map<TypeOperation, String> LIBELLES = new EnumMap<>(TypeOperation.class);

    static {
        ETAPES.put(TypeOperation.COLLECTE, CodeEtape.COLLECTE);
        ETAPES.put(TypeOperation.RECEPTION_BAC, CodeEtape.RECEPTION);
        ETAPES.put(TypeOperation.RECEPTION_SAC_INFECTE, CodeEtape.RECEPTION);
        ETAPES.put(TypeOperation.TRI, CodeEtape.TRI);
        ETAPES.put(TypeOperation.DEMARRAGE_LAVAGE, CodeEtape.LAVAGE);
        ETAPES.put(TypeOperation.FIN_LAVAGE, CodeEtape.LAVAGE);
        ETAPES.put(TypeOperation.LAVAGE_SAC_INFECTE, CodeEtape.LAVAGE);
        ETAPES.put(TypeOperation.DEMARRAGE_SECHAGE, CodeEtape.SECHAGE);
        ETAPES.put(TypeOperation.FIN_SECHAGE, CodeEtape.SECHAGE);
        ETAPES.put(TypeOperation.PLIAGE, CodeEtape.PLIAGE);
        ETAPES.put(TypeOperation.MISE_EN_STOCKAGE, CodeEtape.STOCKAGE);
        ETAPES.put(TypeOperation.LIVRAISON, CodeEtape.DISTRIBUTION);

        LIBELLES.put(TypeOperation.COLLECTE, "Collecte");
        LIBELLES.put(TypeOperation.RECEPTION_BAC, "Réception du bac");
        LIBELLES.put(TypeOperation.RECEPTION_SAC_INFECTE, "Réception d'un sac infecté");
        LIBELLES.put(TypeOperation.TRI, "Tri : affectation au lot");
        LIBELLES.put(TypeOperation.DEMARRAGE_LAVAGE, "Démarrage du lavage");
        LIBELLES.put(TypeOperation.FIN_LAVAGE, "Fin du lavage");
        LIBELLES.put(TypeOperation.LAVAGE_SAC_INFECTE, "Lavage d'un sac infecté");
        LIBELLES.put(TypeOperation.DEMARRAGE_SECHAGE, "Démarrage du séchage");
        LIBELLES.put(TypeOperation.FIN_SECHAGE, "Fin du séchage");
        LIBELLES.put(TypeOperation.PLIAGE, "Pliage");
        LIBELLES.put(TypeOperation.COLLAGE, "Collage / étiquetage");
        LIBELLES.put(TypeOperation.MISE_EN_STOCKAGE, "Mise en stockage");
        LIBELLES.put(TypeOperation.LIVRAISON, "Livraison");
        LIBELLES.put(TypeOperation.CREATION_RECLAMATION, "Réclamation créée");
        LIBELLES.put(TypeOperation.RESOLUTION_RECLAMATION, "Réclamation résolue");
        LIBELLES.put(TypeOperation.CHANGEMENT_ETAT_BAC, "Changement d'état du bac");
        LIBELLES.put(TypeOperation.CHANGEMENT_ETAT_LINGE, "Changement d'état du linge");
        LIBELLES.put(TypeOperation.DECLARATION_SAC_INFECTE, "Déclaration d'un sac infecté");
        LIBELLES.put(TypeOperation.VERIFICATION_SAC_INFECTE_OK, "Vérification du sac infecté : conforme");
        LIBELLES.put(TypeOperation.VERIFICATION_SAC_INFECTE_ECART, "Vérification du sac infecté : écart");
    }

    private OperationsEtape() {
    }

    public static CodeEtape etape(TypeOperation operation) {
        return operation == null ? null : ETAPES.get(operation);
    }

    public static String libelle(TypeOperation operation) {
        if (operation == null) {
            return "";
        }
        String libelle = LIBELLES.get(operation);
        return libelle != null ? libelle : operation.name();
    }
}
