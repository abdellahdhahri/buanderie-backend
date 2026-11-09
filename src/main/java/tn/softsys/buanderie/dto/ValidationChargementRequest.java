package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class ValidationChargementRequest {

    private String matriculeAgent;
    private Double poids;
}
