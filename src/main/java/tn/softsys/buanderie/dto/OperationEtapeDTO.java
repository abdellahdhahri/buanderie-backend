package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.TypeOperation;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationEtapeDTO {

    private TypeOperation typeOperation;
    private String libelle;
    private CodeEtape etape;
    private String nomEtape;
}
