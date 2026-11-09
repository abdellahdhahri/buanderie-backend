package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class LitRequest {

    private Long chambreId;
    private String code;
    private String reference;
    private String description;
    private String type;
    private Boolean actif;
    private Boolean infecte;
}
