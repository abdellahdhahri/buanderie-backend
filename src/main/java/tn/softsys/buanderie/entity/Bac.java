package tn.softsys.buanderie.entity;

import tn.softsys.buanderie.entity.enums.SourceDonnee;

import tn.softsys.buanderie.entity.enums.CategLinge;

import tn.softsys.buanderie.entity.enums.UsageBac;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "bacs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"linges"})
@ToString(exclude = {"linges"})
public class Bac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;
    private String code;
    @Enumerated(EnumType.STRING)
    private EtatBac etat;

    @Enumerated(EnumType.STRING)
    private TypeBac typebac;

    @Builder.Default
    @OneToMany(mappedBy = "bac", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Linge> linges = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TypeCollecte typeCollecte;
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "observations", length = 500)
    private String observations;
    @Column(name = "poids", nullable = true)
    private Double poids;
    @Column(name = "capacite_max")
    private Double capacite_max;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_bac", length = 20)
    @Builder.Default
    private UsageBac usage = UsageBac.COLLECTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", length = 40)
    private CategLinge categorie;

    @Column(name = "poids_balance")
    private Double poidsBalance;

    @Column(name = "source", length = 60)
    private String source;

    @Column(name = "quantite_declaree")
    private Integer quantiteDeclaree;

    @Column(name = "detail_quantites", length = 300)
    private String detailQuantites;

    @Column(unique = true)
    private String rfidTag;

    @Column(length = 255)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_donnee", length = 20)
    private SourceDonnee sourceDonnee;

    @Column(name = "id_externe", length = 60)
    private String idExterne;

    @Column(name = "date_synchro")
    private LocalDateTime dateSynchro;
    @PrePersist
    public void prePersist() {
        if (this.code == null) {
            this.code = "BAC" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        this.dateCreation = LocalDateTime.now();
        if (this.sourceDonnee == null) {
            this.sourceDonnee = SourceDonnee.INTERNE;
        }


    }
}
