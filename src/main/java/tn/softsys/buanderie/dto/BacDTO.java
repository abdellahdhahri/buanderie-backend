package tn.softsys.buanderie.dto;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.time.LocalDateTime;

@Data
@Builder
public class BacDTO {
    private Long id;
    private String reference;
    private EtatBac etat;
    private Double poids;
    private  String code ;
    private String codebac;
    private Double capacite_max;
    private int nombreLinges;
    private TypeCollecte typeCollecte;
    private LocalDateTime dateCreation;
  //  private ServiceEntity service;
    private TypeBac typebac;
    @Column(name = "observations", length = 500)
    private String observations;
    public static BacDTO from(Bac b) {
        if (b == null) return null;
        return BacDTO.builder()
                .id(b.getId())
                .reference(b.getReference())
                .etat(b.getEtat())
                .poids(b.getPoids())
                .capacite_max(b.getCapacite_max())
                .typebac(b.getTypebac())
                .nombreLinges(
                	    b.getLinges() != null
                	        ? b.getLinges().size()
                	        : 0
                	)                .dateCreation(b.getDateCreation())
                .observations(b.getObservations())
                .build();
    }


    }

