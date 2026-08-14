package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class PliageRequest {

    private String matriculeAgent;

    // elle  Peut contenir un code-barres OU un tag RFID
    private String codeOuTag;
}
