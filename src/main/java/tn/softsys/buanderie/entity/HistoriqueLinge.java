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

    @Column(name = "personnel_id", nullable = false)
    private Long personnelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 40)
    private TypeOperation typeOperation;

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    @Column(length = 500)
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_id")
    private Collecte collecte;

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
