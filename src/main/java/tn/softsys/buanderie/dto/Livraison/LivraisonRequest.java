package tn.softsys.buanderie.dto.Livraison;


import lombok.Data;

import java.util.List;

@Data
public class LivraisonRequest {

    private Long serviceId;

    private String matriculeAgent;

    private List<DistributionRequest> items;

    // RFID ou code-barres
    private List<String> codesOuTags;
}
