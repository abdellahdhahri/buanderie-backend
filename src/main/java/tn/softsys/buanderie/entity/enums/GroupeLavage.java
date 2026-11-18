package tn.softsys.buanderie.entity.enums;

import java.text.Normalizer;

public enum GroupeLavage {
    BLANC, COULEUR;

    /** COULEUR si la désignation contient COULEUR, VERT ou BLEU ; sinon BLANC. */
    public static GroupeLavage depuisDesignation(String designation) {
        if (designation == null) return BLANC;
        String t = Normalizer.normalize(designation, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase();
        if (t.contains("COULEUR") || t.contains("VERT") || t.contains("BLEU")) return COULEUR;
        return BLANC;
    }
}
