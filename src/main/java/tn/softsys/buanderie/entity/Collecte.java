package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatCollecte;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

/**
 * Collecte — version enrichie.
 *
 * Modifications par rapport à l'existant :
 *   + session       → FK CollecteSession (regroupement multi-chambres)
 *   + litIdSqlServer    → ID lit SQL Server ciblé
 *   + chambreIdSqlServer→ ID chambre SQL Server ciblée
 *   + nbAttendus    → nb linges attendus (depuis distribution)
 *   + nbScannésBlanc→ nb blancs scannés
 *   + nbPropres     → nb déclarés propres
 *   + nbInfectésPrésumés → déduits automatiquement
 *   + nbManquants   → confirmés manquants par l'agent
 */
@Entity
@Table(name = "collectes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroCollecte;

    @Column(name = "personnel_id")
    private Long personnelId;

    private Long service;


    @Column(name = "chambre_id_sqlserver")
    private Long chambreIdSqlServer;

    @Column(name = "lit_id_sqlserver")
    private Long litIdSqlServer;

    @Column(name = "code_chambre", length = 30)
    private String codeChambre;
    @Enumerated(EnumType.STRING)
    private TypeCollecte typeCollecte;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    private EtatCollecte etatCollecte;

    private LocalDateTime dateCollecte;


    @Column(name = "nb_attendus")
    private Integer nbAttendus;

    @Column(name = "nb_scannes_blanc")
    private Integer nbScannésBlanc;

    @Column(name = "nb_propres")
    private Integer nbPropres;

    @Column(name = "nb_infectes_presumes")
    private Integer nbInfectésPrésumés;

    @Column(name = "nb_manquants")
    private Integer nbManquants;
    @ManyToOne
    @JoinColumn(name = "session_parent_id")
    private Collecte sessionParent;
    @PrePersist
    protected void onCreate() {
        if (this.dateCollecte == null) this.dateCollecte = LocalDateTime.now();
    }
}
