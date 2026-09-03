package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategMachine;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatTechniqueMachine;
import tn.softsys.buanderie.entity.enums.TypeMachine;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "machines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(unique = true, length = 100)
    private String code;

    @Column(unique = true, length = 30)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeMachine type;

    @Column(length = 100)
    private String nom;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String marque;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_machine", length = 50)
    @Builder.Default
    private CategMachine categorie = CategMachine.LINGES_NORMAL;

    @Column(name = "capacite_kg")
    private Double capaciteKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_technique", length = 40)
    private EtatTechniqueMachine etatTechnique;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_immobilisation", length = 30)
    private EtatImmobilisation etatImmobilisation;

    @Column(length = 100)
    private String localisation;

    @Column(name = "clinique_id")
    private Long cliniqueId;

    @Column(name = "legacy_lot_article_immobilisation_id")
    private Long legacyLotArticleImmobilisationId;

    @Column(name = "num_lot")
    private Long numLot;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_arret")
    private LocalDateTime dateArret;

    private Integer duree;

    @Column(name = "date_test_bowie_dick")
    private LocalDateTime dateTestBowieDick;

    @Column(name = "date_test_vide")
    private LocalDateTime dateTestVide;

    @Column(name = "personnel_nom", length = 150)
    private String personnelNom;

    @Column(name = "nombre_cycles")
    private Integer nombreCycles;
    @Column(length = 255)
    private String designation;

    public String getDesignation() {
        return designation;
    }

    public Long getCliniqueId() {
        return cliniqueId;
    }

    public String getLocalisation() {
        return localisation;
    }

    public EtatImmobilisation getEtatImmobilisation() {
        return etatImmobilisation;
    }

    public EtatTechniqueMachine getEtatTechnique() {
        return etatTechnique;
    }

    public CategMachine getCategorie() {
        return categorie;
    }

    public Double getCapaciteKg() {
        return capaciteKg;
    }

    public String getMarque() {
        return marque;
    }


    public String getDescription() {
        return description;
    }

    public String getNom() {
        return nom;
    }

    public TypeMachine getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    public String getCode() {
        return code;
    }

    public Long getId() {
        return id;
    }

    public Long getLegacyLotArticleImmobilisationId() {
        return legacyLotArticleImmobilisationId;
    }

    public Long getNumLot() {
        return numLot;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public LocalDateTime getDateArret() {
        return dateArret;
    }

    public LocalDateTime getDateTestBowieDick() {
        return dateTestBowieDick;
    }

    public Integer getDuree() {
        return duree;
    }

    public LocalDateTime getDateTestVide() {
        return dateTestVide;
    }

    public String getPersonnelNom() {
        return personnelNom;
    }

    public Integer getNombreCycles() {
        return nombreCycles;
    }
}
