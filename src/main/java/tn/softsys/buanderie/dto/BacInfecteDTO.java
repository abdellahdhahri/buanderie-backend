package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacInfecteDTO {

    private Long bacId;
    private String codeBac;
    private Long collecteId;
    private Long serviceId;
    private int quantiteTotale;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String categorie;
        private Integer quantiteDeclaree;
    }
}
