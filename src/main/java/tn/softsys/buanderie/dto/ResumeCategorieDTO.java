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
public class ResumeCategorieDTO {

    private CategLinge categorie;

    private Integer quantite;
}