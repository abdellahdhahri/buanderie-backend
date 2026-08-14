package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeOperation;

/**
 * Journal complet de chaque bac.
 * Chaque changement d'état est signé par l'agent qui l'a effectué.
 */
@Entity
@Table(name = "historiques_bac")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "bac")
@ToString(exclude = "bac")
public class HistoriqueBac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id", nullable = false)
    @JsonIgnore
    private Bac bac;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_avant", length = 30)
    private EtatBac etatAvant;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_apres", nullable = false, length = 30)
    private EtatBac etatApres;

    /** Agent qui a effectué l'opération */
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

    @PrePersist
    protected void onCreate() {
        if (this.dateOperation == null) {
            this.dateOperation = LocalDateTime.now();
        }
    }
}
