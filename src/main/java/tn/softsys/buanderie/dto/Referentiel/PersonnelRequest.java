package tn.softsys.buanderie.dto.Referentiel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonnelRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @Size(max = 100)
    private String prenom;

    @Size(max = 60)
    private String matricule;

    @NotBlank(message = "Le code-barres du badge est obligatoire")
    @Size(max = 60)
    private String codeBarre;

    @Size(max = 120)
    private String fonction;

    private Long fonctionId;

    private Long serviceId;

    private Boolean actif;
}
