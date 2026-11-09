package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class ChambreRequest {

    private String code;
    private String codeBarre;
    private Long serviceId;
    private Boolean actif;
    private Boolean creerLitParDefaut;
}
