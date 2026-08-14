package tn.softsys.buanderie.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardDTO {

    // Compteurs par état de bac
    private long bacsEnAttenteCollecte;
    private long bacsCollectes;
    private long bacsEnLavage;
    private long bacsEnSechage;
    private long bacsEnPliage;
    private long bacsEnCollage;
    private long bacsEnStockage;
    private long bacsLivres;
    private long bacsTotaux;

    // Machines
    private long machinesDisponibles;
    private long machinesEnCours;
    private long machinesEnMaintenance;

    // Réclamations
    private long reclamationsOuvertes;
    private long reclamationsUrgentes;

    // Activité agents aujourd'hui
    private List<ActiviteAgentDTO> activiteAgents;

    // Répartition par catégorie de linge
    private Map<String, Long> repartitionCategories;

    @Data
    @Builder
    public static class ActiviteAgentDTO {
        private String matricule;
        private String nomComplet;
        private long nombreOperations;
        private String derniereOperation;
    }
}
