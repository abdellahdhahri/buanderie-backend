package tn.softsys.buanderie.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.TypeReclamation;

@Data
public class ReclamationRequest {

    @JsonAlias({"codeBarre", "code", "rfidTag"})
    private String codeOuTag;

    private String matriculeAgent;
    private TypeReclamation type;
}