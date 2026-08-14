package tn.softsys.buanderie.dto.Linge;

import lombok.Data;

@Data
public class AssocierRfidRequest {

    private String codeBarre;
    private String codeRfid;
}