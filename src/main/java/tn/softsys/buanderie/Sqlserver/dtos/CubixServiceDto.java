package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CubixServiceDto {

    @JsonProperty("ID")
    private Long id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("CliniqueID")
    private Long cliniqueId;

    @JsonProperty("TypeService")
    private Integer typeService;

    @JsonProperty("ParentServiceID")
    private Long parentServiceId;

    @JsonProperty("ResponsableID")
    private Long responsableId;

    @JsonProperty("Ref")
    private String reference;

    @JsonProperty("TAnalytique")
    private String tAnalytique;

    @JsonProperty("PrestataireID")
    private Long prestataireId;

    @JsonProperty("EstServiceParDefault")
    private Boolean estServiceParDefault;

    @JsonProperty("estBE")
    private Boolean estBE;
}