package tn.softsys.buanderie.entity.enums;

public enum EtatSac {
    DECLARE,          // Déclaré par l'agent de collecte
    RECEPTIONNE,      // Reçu par l'agent de réception
    EN_LAVAGE,        // Vidé dans la machine de lavage
    VERIFIE,          // Vérifié après lavage - quantités concordantes
    ECART_DETECTE,    // Écart détecté entre déclaration et comptage post-lavage
    LINGES_AFFECTES   // Linges propres scannés et réintégrés dans le flux normal
}
