package tn.softsys.buanderie.dto.Livraison;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
public class DistributionItemRequest {

    private CategLinge categorie;

    private Integer quantiteDemandee;
}
