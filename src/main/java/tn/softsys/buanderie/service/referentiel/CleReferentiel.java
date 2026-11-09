package tn.softsys.buanderie.service.referentiel;

import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

public final class CleReferentiel {

    private CleReferentiel() {
    }

    public static Long cle(Arsenal arsenal) {
        return cle(arsenal.getSource(), arsenal.getIdExterne(), arsenal.getId());
    }

    public static Long cle(Emplacement emplacement) {
        return cle(emplacement.getSource(), emplacement.getIdExterne(), emplacement.getId());
    }

    public static Long cle(Chambre chambre) {
        return cle(chambre.getSource(), chambre.getIdExterne(), chambre.getId());
    }

    public static Long cle(Lit lit) {
        return cle(lit.getSource(), lit.getIdExterne(), lit.getId());
    }

    private static Long cle(SourceDonnee source, String idExterne, Long id) {
        if (source == SourceDonnee.EXTERNE && idExterne != null) {
            try {
                return Long.parseLong(idExterne.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return id;
    }
}
