package tn.softsys.buanderie.dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.Bac;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.entity.enums.TypeCollecte;
import tn.softsys.buanderie.entity.enums.UsageBac;

@Data
@Builder
public class BacDTO {
    private Long id;
    private String reference;
    private EtatBac etat;
    private Double poids;
    private String code;
    private String codebac;
    private Double capacite_max;
    private int nombreLinges;
    private TypeCollecte typeCollecte;
    private LocalDateTime dateCreation;
    private TypeBac typebac;
    @Column(name = "observations", length = 500)
    private String observations;
    private UsageBac usage;
    private CategLinge categorie;
    private Double poidsBalance;
    private String source;
    private Integer quantiteDeclaree;
    private String detailQuantites;
    private String designation;
    private String sourceDonnee;
    private String idExterne;

    public static BacDTO from(Bac b) {
        if (b == null) return null;
        return BacDTO.builder()
                .id(b.getId())
                .reference(b.getReference())
                .code(b.getCode())
                .etat(b.getEtat())
                .poids(b.getPoids())
                .capacite_max(b.getCapacite_max())
                .typeCollecte(b.getTypeCollecte())
                .typebac(b.getTypebac())
                .usage(b.getUsage())
                .categorie(b.getCategorie())
                .poidsBalance(b.getPoidsBalance())
                .source(b.getSource())
                .quantiteDeclaree(b.getQuantiteDeclaree())
                .detailQuantites(b.getDetailQuantites())
                .designation(b.getDesignation())
                .sourceDonnee(b.getSourceDonnee() == null ? null : b.getSourceDonnee().name())
                .idExterne(b.getIdExterne())
                .nombreLinges(b.getLinges() != null ? b.getLinges().size() : 0)
                .dateCreation(b.getDateCreation())
                .observations(b.getObservations())
                .build();
    }
}
