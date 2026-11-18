package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.StatutEnvoi;
import tn.softsys.buanderie.entity.enums.TypeOperation;

/** Passage d'étape non reporté dans CUBIX (échec) : conservé pour être renvoyé. Les envois réussis ne sont pas stockés. */
@Entity
@Table(name = "envois_cubix")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvoiCubix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "linge_id")
    private Long lingeId;

    @Column(name = "linge_id_externe", length = 60, nullable = false)
    private String lingeIdExterne;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", length = 40, nullable = false)
    private TypeOperation typeOperation;

    /** Catégorie de l'étape CUBIX : 2 tri, 5 pliage, 6 stockage, 7 livraison. */
    @Column(nullable = false)
    private int categorie;

    @Column(name = "personnel_id_externe", length = 60)
    private String personnelIdExterne;

    @Column(name = "bac_id_externe", length = 60)
    private String bacIdExterne;

    @Column(name = "chambre_code_barre", length = 100)
    private String chambreCodeBarre;

    @Column(name = "livraison_personnel_id_externe", length = 60)
    private String livPersonnelIdExterne;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatutEnvoi statut;

    private int tentatives;

    @Column(length = 1000)
    private String erreur;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_dernier_essai")
    private LocalDateTime dateDernierEssai;
}
