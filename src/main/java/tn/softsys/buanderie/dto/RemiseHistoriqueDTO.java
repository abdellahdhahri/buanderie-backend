package tn.softsys.buanderie.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemiseHistoriqueDTO {

    private Long id;
    private String numeroLivraison;
    private LocalDateTime dateLivraison;
    private String etat;
    private boolean cloturee;

    private Long serviceId;
    private String serviceNom;

    private Long personnelId;
    private String personnelNom;

    private Long collecteId;
    private String numeroCollecte;

    private int totalCollecte;
    private int totalRemis;
    private int totalRestant;

    /** Détail par catégorie (renseigné seulement pour le détail d'une remise). */
    private List<Ligne> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ligne {
        private String categorie;
        private int quantiteCollectee;
        private int quantiteRemise;
        private int quantiteRestante;
    }
}
