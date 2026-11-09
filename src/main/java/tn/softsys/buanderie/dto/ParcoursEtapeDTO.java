package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.EtatLinge;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcoursEtapeDTO {

    private Long id;
    private CodeEtape code;
    private String nom;
    private Integer ordre;
    private boolean actif;
    private boolean obligatoire;
    private List<CodeEtape> suivantes;
    private List<CodeEtape> precedentes;
    private EtatLinge etatProduit;
    private List<EtatLinge> etatsAcceptes;
}
