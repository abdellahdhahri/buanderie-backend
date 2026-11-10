package tn.softsys.buanderie.dto.Lot;

import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotScanResponse {

    private String codeLot;

    private CategLinge categorie;

    private Double poidsRestant;

    private Integer quantiteRestante;
    private Integer duree;

    private String programmeRecommande;
    private Integer vitesse;
    private Integer temperature;
    private String description;

    private String origineProgramme;
    private Boolean programmeParDefaut;
    private Long programmeId;
}