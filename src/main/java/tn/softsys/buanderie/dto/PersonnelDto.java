package tn.softsys.buanderie.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelDto {

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Nom")
    private String nom;

    @JsonProperty("Prenom")
    private String prenom;

    @JsonProperty("NomPrenom")
    private String nomPrenom;

    @JsonProperty("Matricule")
    private String matricule;

    @JsonProperty("CodeBarre")
    private String codeBarre;

    @JsonProperty("Fonction")
    private String fonction;

    @JsonProperty("FonctionID")
    private Long fonctionId;

    @JsonProperty("ServiceID")
    private Long serviceId;

    @JsonProperty("PointeuseActif")
    private Boolean pointeuseActif;

    @JsonProperty("Administrateur")
    private Boolean administrateur;

    @JsonProperty("Visible")
    private Boolean visible;

    @JsonProperty("estMasquer")
    private Boolean estMasquer;
}