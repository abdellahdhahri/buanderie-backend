package tn.softsys.buanderie.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class ChambreDto {
    @JsonProperty("Id")
    private Long id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("ServiceId")
    private Long serviceId;

    @JsonProperty("CodeBarre")
    private String codeBarre;
}
