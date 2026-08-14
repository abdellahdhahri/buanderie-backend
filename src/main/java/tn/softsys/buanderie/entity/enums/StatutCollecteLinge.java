package tn.softsys.buanderie.entity.enums;

public enum StatutCollecteLinge {
    BLANC,            // Scanné comme sale normal → circuit lavage normal
    PROPRE,           // Déclaré propre → laissé en place
    INFECTE_PRESUME,  // Déduit par élimination → mis dans sac infecté
    MANQUANT
}
