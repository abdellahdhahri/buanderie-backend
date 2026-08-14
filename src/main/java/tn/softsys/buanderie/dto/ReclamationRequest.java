package tn.softsys.buanderie.dto;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.TypeReclamation;

@Data
public class ReclamationRequest {

    private String codeOuTag;
    private String matriculeAgent;
    private TypeReclamation type;
}