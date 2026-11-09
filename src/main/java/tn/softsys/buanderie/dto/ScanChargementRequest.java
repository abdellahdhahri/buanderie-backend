package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class ScanChargementRequest {

    private String codeLot;
    private String codeOuTag;
    private String matriculeAgent;
}
