package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacScanResponse {

    private String codeBac;
    private CategLinge categorie;
    private Double poids;
    private Double poidsBalance;
    private Integer nbLinges;
    private Integer duree;
    private String programmeRecommande;
    private Integer vitesse;
    private Integer temperature;
    private String description;
    private String origineProgramme;
    private Boolean programmeParDefaut;
    private Long programmeId;
}
