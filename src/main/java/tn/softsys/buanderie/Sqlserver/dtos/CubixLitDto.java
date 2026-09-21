package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CubixLitDto {

    @JsonAlias("Id")
    private Long id;

    @JsonAlias("Code")
    private String code;

    @JsonAlias("Description")
    private String description;

    @JsonAlias("ChambreId")
    private Long chambreId;

    @JsonAlias("Etat")
    private Integer etat;
}