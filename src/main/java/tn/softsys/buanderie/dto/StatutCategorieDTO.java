package tn.softsys.buanderie.dto;

import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
@Builder
public class StatutCategorieDTO {

    private CategLinge categorie;

    private Integer quantiteCollectee;

    private Integer quantiteLivree;

    private Integer quantiteRestante;
}