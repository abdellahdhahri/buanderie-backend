package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Autoclave", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Autoclave {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "CodeBarre")
    private String codeBarre;

    @Column(name = "Description")
    private String description;

    @Column(name = "Emplacement")
    private String emplacement;

    @Column(name = "CliniqueID")
    private Long cliniqueId;

    @Column(name = "Nom")
    private String nom;

    @Column(name = "LotArticle_ImmobilistationID")
    private Long lotArticleImmobilisationId;

    @Column(name = "Categorie")
    private Integer categorie;

    @Column(name = "NumLot")
    private Long numLot;

    @Column(name = "Etat")
    private Integer etat;

    @Column(name = "DateDebut")
    private LocalDateTime dateDebut;

    @Column(name = "DateArret")
    private LocalDateTime dateArret;

    @Column(name = "Duree")
    private Integer duree;

    @Column(name = "DateTestBowieDick")
    private LocalDateTime dateTestBowieDick;

    @Column(name = "DateTestVide")
    private LocalDateTime dateTestVide;

    @Column(name = "PersonnelNom")
    private String personnelNom;

    @Column(name = "NCycle")
    private Integer nCycle;
}