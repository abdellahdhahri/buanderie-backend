package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.Data;

@Data
public class BacInfecteRequest {

    private String matriculeAgent;
    private Long serviceId;
    private String chambre;
    private String observations;
    private Long bacId;
    private List<BacInfecteItemRequest> items;
}
