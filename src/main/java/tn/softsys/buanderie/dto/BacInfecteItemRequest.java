package tn.softsys.buanderie.dto;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
public class BacInfecteItemRequest {

    private CategLinge categorie;
    private Integer quantiteDeclaree;
}
