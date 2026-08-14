package tn.softsys.buanderie.dto;

import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeOperation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vue complète d'un bac pour la page de tracking.
 * Contient tout l'historique des opérations avec les agents.
 */
@Data
@Builder
public class TrackingDTO {

    private Long bacId;
    private String bacReference;
    private EtatBac etatActuel;
    private String etatActuelLabel;
    private String chambreNumero;

    // Dernière opération
    private String dernierAgentMatricule;
    private String dernierAgentNom;
    private LocalDateTime derniereOperation;
    private TypeOperation dernierTypeOperation;

    // Opérations du cycle courant
    private EtapeDTO reception;
    private EtapeDTO lavage;
    private EtapeDTO sechage;
    private EtapeDTO pliage;
    private EtapeDTO collage;
    private EtapeDTO stockage;
    private EtapeDTO livraison;

    // Journal complet
    private List<EvenementDTO> journal;

    @Data
    @Builder
    public static class EtapeDTO {
        private boolean effectuee;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private String agentMatricule;
        private String agentNom;
        private String details; // machine, programme, emplacement…
    }

    @Data
    @Builder
    public static class EvenementDTO {
        private LocalDateTime date;
        private TypeOperation operation;
        private String operationLabel;
        private String agentMatricule;
        private String agentNom;
        private EtatBac etatAvant;
        private EtatBac etatApres;
        private String observations;
    }
}
