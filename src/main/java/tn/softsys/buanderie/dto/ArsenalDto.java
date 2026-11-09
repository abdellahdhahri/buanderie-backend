package tn.softsys.buanderie.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArsenalDto {

    @JsonProperty("ArsenalID")
    private Long arsenalID;

    @JsonProperty("Designation")
    private String designation;

    @JsonProperty("CodeBarre")
    private String codeBarre;
}
