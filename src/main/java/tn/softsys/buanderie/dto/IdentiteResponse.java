package tn.softsys.buanderie.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.TypeEtablissement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentiteResponse {

    private String raisonSociale;
    private String nomApplication;
    private TypeEtablissement typeEtablissement;
    private Boolean circuitInfecteActif;
    private String couleurPrimaire;
    private String adresse;
    private String telephone;
    private String email;
    private String piedDePage;
    private String logoUrl;
    private Long logoVersion;
    private LocalDateTime dateModification;
}
