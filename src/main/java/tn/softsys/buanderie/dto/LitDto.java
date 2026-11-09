package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LitDto {

    private Long id;
    private String code;
    private String description;
    private Long chambreId;
    private Integer etat;
    private Boolean infecte;
}
