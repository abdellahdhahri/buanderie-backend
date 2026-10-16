package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Entity
@Table(
        name = "remise_linge_linges",
        uniqueConstraints = @UniqueConstraint(columnNames = {"remise_linge_id", "linge_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemiseLingeLinge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remise_linge_id", nullable = false)
    private RemiseLinge remiseLinge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linge_id", nullable = false)
    private Linge linge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategLinge categorie;

    @Column(nullable = false)
    private LocalDateTime dateScan;

    private LocalDateTime dateRangement;

    @PrePersist
    protected void onCreate() {
        if (dateScan == null) {
            dateScan = LocalDateTime.now();
        }
    }
}
