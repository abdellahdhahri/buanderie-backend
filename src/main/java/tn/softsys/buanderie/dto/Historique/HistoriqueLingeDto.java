package tn.softsys.buanderie.dto.Historique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.EtatLinge;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueLingeDto {

    private Long id;

    private Long lingeId;
    private String codeLinge;
    private EtatLinge etatapres;
    private Long agentId;
    private String agentNom;

    private EtatLinge etatId;
    private String etatLibelle;

    private Long typeOperationId;
    private String typeOperationLibelle;

    private LocalDateTime dateOperation;

    private String observations;
}



