package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
@AllArgsConstructor
public class ComparatifDistributionDTO {

    private CategLinge categorie;

    private Integer quantiteCollectee;

    private Integer quantiteDemandee;

    private Long quantiteLivree;

    private Integer reste;

}