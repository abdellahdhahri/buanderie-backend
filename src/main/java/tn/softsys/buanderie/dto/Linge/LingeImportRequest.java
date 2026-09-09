package tn.softsys.buanderie.dto.Linge;

import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
public class LingeImportRequest {

    private Long id;

    private CategLinge categorie;

    private Long serviceId;


}