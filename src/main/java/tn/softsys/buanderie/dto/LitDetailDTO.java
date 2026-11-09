package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LitDetailDTO {

    private Long id;
    private Long cle;
    private Long chambreId;
    private Long chambreCle;
    private String chambreCode;
    private String code;
    private String reference;
    private String description;
    private String type;
    private Boolean actif;
    private Boolean infecte;
    private String source;

    public static LitDetailDTO depuis(Lit lit) {
        return LitDetailDTO.builder()
                .id(lit.getId())
                .cle(CleReferentiel.cle(lit))
                .chambreId(lit.getChambre().getId())
                .chambreCle(CleReferentiel.cle(lit.getChambre()))
                .chambreCode(lit.getChambre().getCode())
                .code(lit.getCode())
                .reference(lit.getReference())
                .description(lit.getDescription())
                .type(lit.getType())
                .actif(lit.getActif())
                .infecte(lit.getInfecte())
                .source(lit.getSource() != null ? lit.getSource().name() : null)
                .build();
    }
}
