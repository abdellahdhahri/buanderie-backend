package tn.softsys.buanderie.dto;

import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.dto.StatutCategorieDTO;

import java.util.List;

@Data
@Builder
public class StatutCollecteDTO {

    private Long collecteId;

    private Integer totalCollecte;

    private Integer totalLivre;

    private Integer totalRestant;

    private Integer pourcentage;

    private String statut;

    private List<StatutCategorieDTO> categories;
}