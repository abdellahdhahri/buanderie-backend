package tn.softsys.buanderie.dto.SacsInfectes;

import lombok.Data;
import java.util.List;

/**
 * DTO pour scanner et affecter les linges propres sortis du lavage.
 * Appelé après la vérification post-lavage (etat=VERIFIE ou ECART_DETECTE).
 * L'agent scanne chaque linge propre un par un avec son code-barre.
 */
@Data
public class AffectationLingesPropresRequest {

    /** Numéro du sac infecté lavé */
    private String numeroSac;

    /** Matricule de l'agent qui scanne les linges propres */
    private String matriculeAgent;

    /**
     * Codes-barres des linges propres scannés un par un après lavage.
     * Ces codes existent déjà en base (créés lors de la fabrication du linge).
     * Ils étaient SALE/COLLECTE avant, maintenant ils deviennent LAVE.
     */
    private List<String> codesLinges;

    /** Observations optionnelles */
    private String observations;
}
