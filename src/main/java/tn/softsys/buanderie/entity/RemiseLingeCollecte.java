package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "remise_linge_collectes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"remise_linge_id", "collecte_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemiseLingeCollecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remise_linge_id", nullable = false)
    private RemiseLinge remiseLinge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecte_id", nullable = false)
    private Collecte collecte;
}
