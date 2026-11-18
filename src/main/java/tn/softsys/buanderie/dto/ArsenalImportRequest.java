package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.Data;

@Data
public class ArsenalImportRequest {

    private List<Long> ids;
    private Integer nbrCol;
    private List<String> emplacements;
}
