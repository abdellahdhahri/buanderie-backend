package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CubixLitsResponseDto {

    @JsonProperty("Success")
    private Boolean success;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("ChambreId")
    private Long chambreId;

    @JsonProperty("Nombre")
    private Integer nombre;

    @JsonProperty("Data")
    private List<CubixLitDto> data;
}