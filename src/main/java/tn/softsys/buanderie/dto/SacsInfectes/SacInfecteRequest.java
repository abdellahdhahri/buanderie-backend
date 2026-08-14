package tn.softsys.buanderie.dto.SacsInfectes;

import lombok.Data;

import java.util.List;


@Data
public class SacInfecteRequest {

    private String matriculeAgent;

    private Long serviceId;

    private String chambre;

    private List<SacInfecteItemRequest> items;

    private String observations;
    private Long bacId;
}
