package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class AffectationBacRequest {

    private String codeOuTag;
    private String codeBac;
    private String matriculeAgent;
}
