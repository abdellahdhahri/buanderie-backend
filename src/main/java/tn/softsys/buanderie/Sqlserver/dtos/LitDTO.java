package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LitDTO {
    private Long id;
    private String code;
    private String description;
    private Long chambreId;
    private Integer etat;
    private Boolean infecte;

}
