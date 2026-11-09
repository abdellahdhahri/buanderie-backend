package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmplacementDetailDTO {

    private Long id;
    private Long cle;
    private Long arsenalId;
    private String arsenalNom;
    private String nom;
    private Integer numero;
    private String codeBarre;
    private Boolean actif;
    private String source;

    public static EmplacementDetailDTO depuis(Emplacement e) {
        return EmplacementDetailDTO.builder()
                .id(e.getId())
                .cle(CleReferentiel.cle(e))
                .arsenalId(e.getArsenal().getId())
                .arsenalNom(e.getArsenal().getNom())
                .nom(e.getNom())
                .numero(e.getNumero())
                .codeBarre(e.getCodeBarre())
                .actif(e.getActif())
                .source(e.getSource() != null ? e.getSource().name() : null)
                .build();
    }
}
