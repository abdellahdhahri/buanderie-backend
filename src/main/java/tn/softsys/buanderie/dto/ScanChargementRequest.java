package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class ScanChargementRequest {

    private String codeBac;
    private String codeOuTag;
    private String matriculeAgent;
}
