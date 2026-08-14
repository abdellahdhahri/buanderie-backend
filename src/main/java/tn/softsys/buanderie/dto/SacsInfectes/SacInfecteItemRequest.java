package tn.softsys.buanderie.dto.SacsInfectes;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;


@Data
public class SacInfecteItemRequest {
    private CategLinge categorie;
    private Integer quantiteDeclaree;
}
