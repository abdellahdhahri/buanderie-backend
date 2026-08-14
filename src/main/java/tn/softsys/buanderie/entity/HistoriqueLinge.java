package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;

import java.time.LocalDateTime;

@Entity
@Table(name = "historiques_linge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "linge")
@ToString(exclude = "linge")
public class HistoriqueLinge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linge_id", nullable = false)
    private Linge linge;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_avant", length = 30)
    private EtatLinge etatAvant;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_apres", nullable = false, length = 30)
    private EtatLinge etatApres;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 40)
    private TypeOperation typeOperation;

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    @Column(length = 500)
    private String observations;

    /** Lien vers la collecte parente — traçabilité complète linge → collecte → agent */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_id")
    private Collecte collecte;

    /** Lien vers le sac infecté — traçabilité complète linge infecté → sac → collecte */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sac_infecte_id")
    private SacInfecte sacInfecte;

    @PrePersist
    protected void onCreate() {
        if (this.dateOperation == null) {
            this.dateOperation = LocalDateTime.now();
        }
    }
}
