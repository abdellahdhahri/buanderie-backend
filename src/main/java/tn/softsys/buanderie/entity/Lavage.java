package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Session de lavage : un bac + une machine + un programme + un agent.
 * C'est l'entité centrale de la traçabilité du processus de lavage.
 */
@Entity
@Table(name = "lavages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lavage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id", nullable = false)
    private Bac bac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_lavage_id", nullable = false)
    private ProgrammeLavage programmeLavage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String statut = "EN_COURS"; // EN_COURS, TERMINE, ANNULE

    @Column(length = 300)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (this.dateDebut == null) {
            this.dateDebut = LocalDateTime.now();
        }
    }
}
