package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sechages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sechage {

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
    @JoinColumn(name = "programme_sechage_id", nullable = false)
    private ProgrammeSechage programmeSechage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String statut = "EN_COURS";

    @Column(length = 300)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (this.dateDebut == null) {
            this.dateDebut = LocalDateTime.now();
        }
    }
}
