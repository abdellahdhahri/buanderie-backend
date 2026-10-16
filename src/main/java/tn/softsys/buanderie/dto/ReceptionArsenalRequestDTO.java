
package tn.softsys.buanderie.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import tn.softsys.buanderie.entity.enums.CategLinge;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionArsenalRequestDTO {

    @NotNull
    @Positive
    private Long remiseLingeId;

    @NotNull
    @Positive
    private Long arsenalID;

    @NotEmpty
    @Valid
    private List<ArticleRecuDTO> articles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArticleRecuDTO {

        @NotNull
        private CategLinge categorie;

        @NotNull
        @PositiveOrZero
        private Integer quantiteRecue;

        @NotEmpty
        @Valid
        private List<RangementDTO> rangements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RangementDTO {

        @NotNull
        @Positive
        private Long emplacementID;

        @NotNull
        @Positive
        private Integer quantiteRangee;
    }
}
