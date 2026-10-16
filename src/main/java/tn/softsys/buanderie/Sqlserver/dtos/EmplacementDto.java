
package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmplacementDto {

    @JsonProperty("EmplacementID")
    private Long emplacementID;

    @JsonProperty("ArsenalID")
    private Long arsenalID;

    @JsonProperty("Nom")
    private String nom;

    @JsonProperty("N")
    private Integer numero;

    @JsonProperty("CodeBarre")
    private String codeBarre;
}
