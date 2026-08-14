package tn.softsys.buanderie.dto.SacsInfectes;

import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatSac;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SacInfecteDTO {

    private Long id;
    private String numeroSac;
    private EtatSac etat;

    /** ID de la collecte parente → permet de remonter à l'agent et au service */
    private Long collecteId;

    private String agentCollecteNom;
    private String agentCollecteMatricule;
    private String serviceNom;
    private String chambre;

    private Integer quantiteDeclaree;
    private Integer quantiteCompteePostLavage;
    private Integer ecart;
    private boolean concordance;

    private LocalDateTime dateCollecte;
    private LocalDateTime dateReception;
    private LocalDateTime dateLavage;
    private LocalDateTime dateVerification;

    private String agentLavageNom;
    private String observations;

    private List<ItemDTO> items;
    private List<String> codesLingesPropres;
    private int nombreLingesAffectes;

    @Data
    @Builder
    public static class ItemDTO {
        private CategLinge categorie;
        private Integer quantiteDeclaree;
        private Integer quantiteComptee;
        private Integer ecart;
    }
}
