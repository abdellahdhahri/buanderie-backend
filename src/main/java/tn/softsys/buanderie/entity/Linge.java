package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.StatutCollecteLinge;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

@Entity
@Table(name = "linges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "bac")
@ToString(exclude = "bac")
public class Linge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Schema(description = "Date et heure prévue de livraison")
    private LocalDateTime dateLivraisonPreview;
    @Schema(description = "Poids en kg", example = "2.5")
    private Double poids;
    @Column(name = "code_barre", unique = true, nullable = false, length = 50)
    private String codeBarre;

    @Column(unique = true, nullable = true)
    private String rfidTag;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategLinge categorie;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EtatLinge etat = EtatLinge.PROPRE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id")
    @JsonIgnore
    private Bac bac;

@Column(name = "lit_id_sqlserver")
private Long litIdSqlServer;

    @Column(name = "chambre_id_sqlserver")
    private Long chambreIdSqlServer;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "porteur_id")
    private Long porteurId;

    @Column(name = "porteur_nom", length = 100)
    private String porteurNom;

    @ManyToOne(optional = true)
    @JoinColumn(name = "lot_id",nullable = true)
    @JsonIgnore
    private Lot lot;

    private Integer dureVie;
    @Enumerated(EnumType.STRING)
    @Column(name = "type_collecte")

    private TypeCollecte typeCollecte;

    private LocalDateTime dateCreation;

    @Column(name = "nombreLavage",nullable=true)
     private int nbLavage = 0;
    @Column(name = "observations", length = 500)
    private String observations;

    @Column(name = "programme_lavage")
    private String programmeLavage;

    @Column(name = "programme_sechage")
    private String programmeSechage;

    @ManyToOne(optional = true)
    @JoinColumn(name = "arsenal_id",nullable = true)
    @JsonIgnore
    private Arsenal emplacement;
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_collecte", length = 30)
    private StatutCollecteLinge statutCollecte;
    @PrePersist
    public void prePersist() {
        if (this.codeBarre == null) {
            this.codeBarre = "L" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        if (this.etat == null) {
            this.etat = EtatLinge.COLLECTE;
        }
        if (this.codeBarre == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }

}
