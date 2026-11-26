package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargementDTO {

    private String codeBac;
    private EtatBac etatBac;
    private CategLinge categorie;
    private TypeCollecte typeCollecte;
    private String groupeLavage;
    private int nbLinges;
    private double poidsDeclare;
    private Double poidsBalance;
    private Double ecartPourcent;
    private boolean pretPourLavage;
    private List<String> codesLinges;
    private List<String> messages;
}
