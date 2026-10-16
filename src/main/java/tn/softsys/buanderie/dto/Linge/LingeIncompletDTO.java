package tn.softsys.buanderie.dto.Linge;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LingeIncompletDTO {

    private Long id;

    private String codeBarre;

    private String designation;

    private String categorie;

    private String etat;

    private Long serviceId;

    private Double poids;

    private Integer dureVie;

    private List<String> attributsManquants;
}
