package tn.softsys.buanderie.Sqlserver.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LitLibreDTO {
    private Long litId;
    private String litCode;

    private Long chambreId;
    private String chambreCode;

    private Integer etatChambre;
}
