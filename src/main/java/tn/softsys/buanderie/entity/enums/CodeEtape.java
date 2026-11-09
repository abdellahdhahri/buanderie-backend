package tn.softsys.buanderie.entity.enums;

public enum CodeEtape {

    COLLECTE("Collecte", Zone.ZONE_SALE, 10, true, "LIG1501", EtatLinge.COLLECTE),
    RECEPTION("Réception", Zone.ZONE_SALE, 20, true, "LIG1502", EtatLinge.RECEPTION),
    TRI("Tri", Zone.ZONE_SALE, 30, false, "LIG1503", EtatLinge.EN_TRI),
    LAVAGE("Lavage", Zone.ZONE_SALE, 40, true, "LIG1504", EtatLinge.LAVE),
    SECHAGE("Séchage", Zone.ZONE_PROPRE, 50, false, "LIG1601", EtatLinge.SECHE),
    PLIAGE("Pliage", Zone.ZONE_PROPRE, 60, false, "LIG1602", EtatLinge.PLIE),
    STOCKAGE("Stockage", Zone.ZONE_PROPRE, 70, false, "LIG1603", EtatLinge.STOCKE),
    DISTRIBUTION("Distribution", Zone.ZONE_PROPRE, 80, true, "LIG1605", EtatLinge.LIVRE);

    private final String nom;
    private final Zone zone;
    private final int ordreParDefaut;
    private final boolean obligatoire;
    private final String privilegeRequis;
    private final EtatLinge etatProduit;

    CodeEtape(String nom, Zone zone, int ordreParDefaut, boolean obligatoire,
              String privilegeRequis, EtatLinge etatProduit) {
        this.nom = nom;
        this.zone = zone;
        this.ordreParDefaut = ordreParDefaut;
        this.obligatoire = obligatoire;
        this.privilegeRequis = privilegeRequis;
        this.etatProduit = etatProduit;
    }

    public String getNom() {
        return nom;
    }

    public Zone getZone() {
        return zone;
    }

    public int getOrdreParDefaut() {
        return ordreParDefaut;
    }

    public boolean isObligatoire() {
        return obligatoire;
    }

    public String getPrivilegeRequis() {
        return privilegeRequis;
    }

    public EtatLinge getEtatProduit() {
        return etatProduit;
    }
}
