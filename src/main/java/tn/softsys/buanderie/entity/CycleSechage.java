package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.StatutCycle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "cycles_sechage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSechage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cycle", unique = true)
    private String numeroCycle;

    // Séchoir seul uniquement (typeMachine = SECHEUSE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    // Programme suggéré au 1er linge chargé, verrouillé ensuite
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_sechage_id")
    private ProgrammeSechage programmeSechage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutCycle statut = StatutCycle.EN_COURS;

    @Column(name = "poids_charge")
    @Builder.Default
    private Double poidsCharge = 0.0;

    @OneToMany(mappedBy = "cycleSechage",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<CycleSechageItem> items = new ArrayList<>();

    @Column(length = 300)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (numeroCycle == null)
            numeroCycle = "CS-" + System.currentTimeMillis();
        if (dateDebut == null)
            dateDebut = LocalDateTime.now();
        if (statut == null)
            statut = StatutCycle.EN_COURS;
    }
}
