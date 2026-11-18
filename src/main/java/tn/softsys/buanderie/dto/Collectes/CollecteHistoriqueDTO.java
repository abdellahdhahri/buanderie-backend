package tn.softsys.buanderie.dto.Collectes;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class CollecteHistoriqueDTO {

    private Long   id;
    private String numeroCollecte;
    private String typeCollecte;
    private String etatCollecte;

    private String agentMatricule;
    private String agentNom;

    private Long   serviceId;
    private String serviceCode;

    private Long   chambreId;
    private String codeChambre;

    private LocalDateTime dateCollecte;
    private LocalDateTime dateFin;

    private Integer nbAttendus;
    private Integer nbBlancs;
    private Integer nbInfectes;
    private Integer nbPropres;
    private Integer nbManquants;
    private Integer nbTotal;

    private List<CollecteLingeDTO> linges;
    private List<BacInfecteResumeDTO> bacsInfectes;

    @Data @Builder
    public static class CollecteLingeDTO {
        private Long   lingeId;
        private String codeBarre;
        private String rfidTag;
        private String categorie;
        private String etatActuel;
        private String typeCollecte;
        private String statutCollecte;
        private LocalDateTime dateScan;
        private String codeBac;
        private String numeroSacInfecte;
    }

    @Data @Builder
    public static class BacInfecteResumeDTO {
        private Long    bacId;
        private String  codeBac;
        private Integer quantiteDeclaree;
        private LocalDateTime dateDeclaration;
        private List<ItemDTO> items;

        @Data @Builder
        public static class ItemDTO {
            private String  categorie;
            private Integer quantiteDeclaree;
        }
    }
}
