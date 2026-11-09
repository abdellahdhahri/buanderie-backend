package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.Zone;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LingeEtapeDTO {

    private Long id;
    private CodeEtape code;
    private String nom;
    private String codeBarre;
    private String categorie;
    private Zone zone;
    private Integer ordre;
    private boolean actif;
    private boolean obligatoire;
    private boolean standard;
    private String privilegeRequis;
    private List<CodeEtape> suivantes;

    public static LingeEtapeDTO depuis(LingeEtape e, List<CodeEtape> suivantes) {
        return LingeEtapeDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .nom(e.getNom())
                .codeBarre(e.getCodeBarre())
                .categorie(e.getCategorie())
                .zone(e.getZone())
                .ordre(e.getOrdre())
                .actif(!Boolean.FALSE.equals(e.getActif()))
                .obligatoire(Boolean.TRUE.equals(e.getObligatoire()))
                .standard(e.getCode() != null)
                .privilegeRequis(e.getPrivilegeRequis())
                .suivantes(suivantes)
                .build();
    }
}
