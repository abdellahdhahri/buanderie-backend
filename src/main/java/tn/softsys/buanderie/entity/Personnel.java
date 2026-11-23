package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

@Entity
@Table(
        name = "personnel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_personnel_source_id_externe", columnNames = {"source", "id_externe"})
        },
        indexes = {
                @Index(name = "idx_personnel_code_barre", columnList = "code_barre")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceDonnee source;

    @Column(name = "id_externe", length = 60)
    private String idExterne;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(length = 60)
    private String matricule;

    @Column(name = "code_barre", length = 60)
    private String codeBarre;

    @Column(length = 120)
    private String fonction;

    private Long fonctionId;

    private Long serviceId;

    /** Service d'affectation saisi ici (étage, secteur…) : jamais écrasé par la synchronisation CUBIX. */
    @Column(name = "service_affectation_id")
    private Long serviceAffectationId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    private LocalDateTime dateSynchro;

    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    public String getNomPrenom() {
        String n = nom == null ? "" : nom.trim();
        String p = prenom == null ? "" : prenom.trim();
        return (n + " " + p).trim();
    }

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = dateCreation;
        if (actif == null) {
            actif = true;
        }
        if (source == null) {
            source = SourceDonnee.INTERNE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
