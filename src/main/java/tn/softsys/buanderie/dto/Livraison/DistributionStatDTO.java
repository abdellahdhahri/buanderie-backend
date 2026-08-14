package tn.softsys.buanderie.dto.Livraison;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
@AllArgsConstructor
public class DistributionStatDTO {

    private CategLinge categorie;

    private Integer quantiteCollectee;

    private Integer quantiteDemandee;

    private Long quantiteLivree;

    private Long reste;
}
