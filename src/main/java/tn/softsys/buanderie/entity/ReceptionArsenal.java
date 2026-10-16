
package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "receptions_arsenal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionArsenal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Remise concernée dans notre application
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remise_linge_id", nullable = false)
    private RemiseLinge remiseLinge;

    // Identifiant de l'Arsenal provenant de Cubix
    @Column(name = "arsenal_id", nullable = false)
    private Long arsenalID;

    @Column(name = "date_reception", nullable = false)
    private LocalDateTime dateReception;

    // Statut propre à la réception dans l'Arsenal
    @Column(name = "statut_reception", nullable = false, length = 30)
    private String statutReception;

    @Column(length = 500)
    private String observations;

    @PrePersist
    protected void onCreate() {
        if (dateReception == null) {
            dateReception = LocalDateTime.now();
        }

        if (statutReception == null) {
            statutReception = "EN_COURS";
        }
    }
}
