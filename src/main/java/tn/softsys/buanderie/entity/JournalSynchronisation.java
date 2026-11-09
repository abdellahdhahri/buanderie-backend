package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.Referentiel;

@Entity
@Table(name = "journal_synchronisations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalSynchronisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Referentiel referentiel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceDonnee source;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    @Column(nullable = false, length = 20)
    private String statut;

    private Integer nbRecus;

    private Integer nbCrees;

    private Integer nbMisAJour;

    private Integer nbDesactives;

    @Column(length = 1000)
    private String message;

    @Column(length = 20)
    private String declencheur;
}
