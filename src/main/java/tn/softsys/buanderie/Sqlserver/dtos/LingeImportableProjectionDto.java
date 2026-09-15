package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LingeImportableProjectionDto {

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Designation")
    private String designation;

    @JsonProperty("CodeBarre")
    private String codeBarre;

    @JsonProperty("EtatActif")
    private Integer etatActif;

    @JsonProperty("PrestataireArticleId")
    private Long prestataireArticleId;

    @JsonProperty("ArticleFamilleId")
    private Long articleFamilleId;

    @JsonProperty("NumeroSerie")
    private String numeroSerie;
}