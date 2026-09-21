package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tn.softsys.buanderie.dto.ChambreDto;

import java.util.List;

@Data
public class ChambresImportResponse {

    @JsonProperty("Success")
    private Boolean success;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Nombre")
    private Integer nombre;

    @JsonProperty("Data")
    private List<ChambreDto> data;
}