package tn.softsys.buanderie.dto;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.List;

/**
 * DTO pour la vérification post-lavage d'un sac infecté.
 * L'agent de lavanderie compte les linges propres sortis de la machine
 * et les saisit ici par catégorie pour comparaison avec la déclaration.
 */
@Data
public class VerificationLavageRequest {

    /** Numéro du sac infecté à vérifier */
    private String numeroSac;

    /** Matricule de l'agent de lavanderie effectuant le comptage */
    private String matriculeAgent;

    /** Comptage réel par catégorie après lavage */
    private List<VerificationItemRequest> itemsComptes;

    /** Observations de l'agent de lavanderie */
    private String observations;

    @Data
    public static class VerificationItemRequest {
        private CategLinge categorie;
        private Integer quantiteComptee;
    }
}
