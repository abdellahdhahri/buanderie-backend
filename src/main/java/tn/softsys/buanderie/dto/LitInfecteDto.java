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
public class LitInfecteDto {

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("ChambreId")
    private Long chambreId;

    @JsonProperty("EstInfecte")
    private Boolean estInfecte;
}