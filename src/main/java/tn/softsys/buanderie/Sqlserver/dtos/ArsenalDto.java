package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArsenalDto {
    private Long id;
    private Integer categorie;
    private Long LotArticle_ImmobilistationID;
}
