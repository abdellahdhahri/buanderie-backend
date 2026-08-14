package tn.softsys.buanderie.Sqlserver.dtos;


import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class PersonnelDto {

    private Long id;
    private String nom;
    private String prenom;
    private String codeBarre;
    private String fonction;
}
