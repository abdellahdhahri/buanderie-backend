package tn.softsys.buanderie.dto.Lot;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

@Data
public class LotDTO {

    @NotNull(message = "Le type de collecte est obligatoire")
    private TypeCollecte typeCollecte;

    @NotNull(message = "La catégorie est obligatoire")
    private CategLinge categorie;

    @NotNull(message = "La capacité maximale est obligatoire")
    @Min(value = 1, message = "La capacité maximale doit être supérieure à 0")
    private Double capaciteMax;
}
