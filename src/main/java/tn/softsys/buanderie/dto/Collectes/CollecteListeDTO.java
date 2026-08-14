package tn.softsys.buanderie.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class CollecteListeDTO {
    private Long   id;
    private String numeroCollecte;
    private String typeCollecte;
    private String etatCollecte;
    private String agentNom;
    private String agentMatricule;
    private Long   serviceId;
    private String serviceCode;
    private String codeChambre;
    private LocalDateTime dateCollecte;
    private LocalDateTime dateFin;
    private Integer nbAttendus;
    private Integer nbBlancs;
    private Integer nbInfectes;
    private Integer nbPropres;
    private Integer nbManquants;
    private Integer nbTotal;
}
