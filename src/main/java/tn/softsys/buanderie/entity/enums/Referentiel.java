package tn.softsys.buanderie.entity.enums;

public enum Referentiel {
    SERVICE(true),
    PERSONNEL(true),
    CHAMBRE(true),
    LIT(true),
    LIT_INFECTE(true),
    ARSENAL(true),
    EMPLACEMENT(true),
    LINGE(true),
    MACHINE(true);

    private final boolean synchronisationDisponible;

    Referentiel(boolean synchronisationDisponible) {
        this.synchronisationDisponible = synchronisationDisponible;
    }

    public boolean isSynchronisationDisponible() {
        return synchronisationDisponible;
    }
}