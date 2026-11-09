package tn.softsys.buanderie.service.referentiel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;

public final class ModeleConnecteur {

    public static final Map<Referentiel, List<String>> CHAMPS = Map.of(
            Referentiel.PERSONNEL, List.of(
                    "idExterne", "nom", "prenom", "matricule", "codeBarre",
                    "fonction", "fonctionId", "serviceId", "actif", "masque"),
            Referentiel.SERVICE, List.of(
                    "idExterne", "code", "nom", "description", "reference", "actif"),
            Referentiel.CHAMBRE, List.of(
                    "idExterne", "code", "codeBarre", "serviceId", "actif"),
            Referentiel.LIT, List.of(
                    "idExterne", "chambreId", "code", "description", "type", "actif"),
            Referentiel.LIT_INFECTE, List.of(
                    "idExterne", "infecte"),
            Referentiel.ARSENAL, List.of(
                    "idExterne", "nom", "code", "serviceId", "actif"),
            Referentiel.EMPLACEMENT, List.of(
                    "idExterne", "arsenalId", "nom", "numero", "codeBarre", "actif"),
            Referentiel.LINGE, List.of(
                    "idExterne", "designation", "codeBarre", "etatActif", "categorie", "serviceId"),
            Referentiel.MACHINE, List.of(
                    "idExterne", "code", "designation", "reference", "etatActif", "type",
                    "marque", "capaciteKg", "localisation")
    );

    private static final Map<Referentiel, String> CHEMINS_CUBIX = Map.of(
            Referentiel.PERSONNEL, "/Importer/BuanderieImport/GetPersonnels",
            Referentiel.SERVICE, "/Importer/BuanderieImport/GetServices",
            Referentiel.CHAMBRE, "/Importer/BuanderieImport/Chambres",
            Referentiel.LIT, "/Importer/BuanderieImport/LitsParChambre",
            Referentiel.LIT_INFECTE, "/Importer/BuanderieImport/GetLitsInfectes",
            Referentiel.ARSENAL, "/Importer/ArsenalImport/GetArsenals",
            Referentiel.EMPLACEMENT, "/Importer/ArsenalImport/GetEmplacements",
            Referentiel.LINGE, "/Importer/BuanderieImport/Linges",
            Referentiel.MACHINE, "/Importer/BuanderieImport/Machines"
    );

    private static final Map<Referentiel, String> CHEMINS_STANDARD = Map.of(
            Referentiel.PERSONNEL, "/personnel",
            Referentiel.SERVICE, "/services",
            Referentiel.CHAMBRE, "/chambres",
            Referentiel.LIT, "/lits",
            Referentiel.LIT_INFECTE, "/lits-infectes",
            Referentiel.ARSENAL, "/arsenaux",
            Referentiel.EMPLACEMENT, "/emplacements",
            Referentiel.LINGE, "/linges",
            Referentiel.MACHINE, "/machines"
    );

    private ModeleConnecteur() {
    }

    public static String chemin(TypeConnecteur type, Referentiel referentiel) {
        return switch (type) {
            case CUBIX -> CHEMINS_CUBIX.get(referentiel);
            case STANDARD -> CHEMINS_STANDARD.get(referentiel);
            case PERSONNALISE -> null;
        };
    }

    public static String cheminListe(TypeConnecteur type, Referentiel referentiel) {
        if (type == TypeConnecteur.CUBIX
                && (referentiel == Referentiel.SERVICE
                || referentiel == Referentiel.LINGE
                || referentiel == Referentiel.MACHINE
                || referentiel == Referentiel.ARSENAL
                || referentiel == Referentiel.EMPLACEMENT
                || referentiel == Referentiel.CHAMBRE
                || referentiel == Referentiel.LIT
                || referentiel == Referentiel.LIT_INFECTE)) {
            return "Data";
        }
        return null;
    }

    public static Map<String, String> correspondance(TypeConnecteur type, Referentiel referentiel) {
        Map<String, String> champs = new LinkedHashMap<>();
        if (referentiel == Referentiel.SERVICE) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "ID");
                    champs.put("code", "Code");
                    champs.put("nom", "Description");
                    champs.put("description", "Description");
                    champs.put("reference", "Ref");
                }
                case STANDARD -> CHAMPS.get(Referentiel.SERVICE).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.LINGE) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "Id");
                    champs.put("designation", "Designation");
                    champs.put("codeBarre", "CodeBarre");
                    champs.put("etatActif", "EtatActif");
                }
                case STANDARD -> CHAMPS.get(Referentiel.LINGE).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.MACHINE) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "Id");
                    champs.put("code", "CodeBarre");
                    champs.put("designation", "Designation");
                    champs.put("reference", "NumeroSerie");
                    champs.put("etatActif", "EtatActif");
                }
                case STANDARD -> CHAMPS.get(Referentiel.MACHINE).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.CHAMBRE) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "Id");
                    champs.put("code", "Code");
                    champs.put("codeBarre", "CodeBarre");
                    champs.put("serviceId", "ServiceId");
                }
                case STANDARD -> CHAMPS.get(Referentiel.CHAMBRE).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.LIT) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "Id");
                    champs.put("chambreId", "ChambreId");
                    champs.put("code", "Code");
                    champs.put("description", "Description");
                }
                case STANDARD -> CHAMPS.get(Referentiel.LIT).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.LIT_INFECTE) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "Id");
                    champs.put("infecte", "EstInfecte");
                }
                case STANDARD -> CHAMPS.get(Referentiel.LIT_INFECTE).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.ARSENAL) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "ArsenalID");
                    champs.put("nom", "Designation");
                    champs.put("code", "CodeBarre");
                }
                case STANDARD -> CHAMPS.get(Referentiel.ARSENAL).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel == Referentiel.EMPLACEMENT) {
            switch (type) {
                case CUBIX -> {
                    champs.put("idExterne", "EmplacementID");
                    champs.put("arsenalId", "ArsenalID");
                    champs.put("nom", "Nom");
                    champs.put("numero", "N");
                    champs.put("codeBarre", "CodeBarre");
                }
                case STANDARD -> CHAMPS.get(Referentiel.EMPLACEMENT).forEach(c -> champs.put(c, c));
                case PERSONNALISE -> {
                }
            }
            return champs;
        }
        if (referentiel != Referentiel.PERSONNEL) {
            return champs;
        }
        switch (type) {
            case CUBIX -> {
                champs.put("idExterne", "Id");
                champs.put("nom", "Nom");
                champs.put("prenom", "Prenom");
                champs.put("matricule", "Matricule");
                champs.put("codeBarre", "CodeBarre");
                champs.put("fonction", "Fonction");
                champs.put("fonctionId", "FonctionID");
                champs.put("serviceId", "ServiceID");
                champs.put("masque", "estMasquer");
            }
            case STANDARD -> CHAMPS.get(Referentiel.PERSONNEL).forEach(c -> champs.put(c, c));
            case PERSONNALISE -> {
            }
        }
        return champs;
    }
}