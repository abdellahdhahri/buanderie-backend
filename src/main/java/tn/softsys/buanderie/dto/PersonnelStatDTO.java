package tn.softsys.buanderie.dto;

import lombok.*;
import tn.softsys.buanderie.entity.enums.TypeOperation;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelStatDTO {

    private Long personnelId;
    private String nomPrenom;
    private String matricule;

    private Long totalOperations;
    private Map<TypeOperation, Long> operationsParType;

    private LocalDateTime premiereOperation;
    private LocalDateTime derniereOperation;

    private Double moyenneParJour;
}