package tn.softsys.buanderie.dto.Lot;

import lombok.Data;

@Data
public class AffectationLotRequest {

    private String codeOuTag;

    private Long lotId;

    private String matriculeAgent;
}
