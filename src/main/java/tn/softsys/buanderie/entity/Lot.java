package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codeLot;

    @Enumerated(EnumType.STRING)
    private TypeCollecte typeCollecte;

    @Enumerated(EnumType.STRING)
    private CategLinge categorie;

    @Enumerated(EnumType.STRING)
    private EtatLot etat;

    private LocalDateTime dateCreation;
    @Builder.Default
    private Double capaciteMax= 10.0;
    @Builder.Default
    private Double poidsActuel = 0.0;  
    @PrePersist
    public void prePersist() {
        dateCreation = LocalDateTime.now();
    }

}