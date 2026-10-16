
package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Entity
@Table(
        name = "stock_arsenal",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"arsenal_id", "emplacement_id", "categorie"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockArsenal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant de l'Arsenal provenant de Cubix
    @Column(name = "arsenal_id", nullable = false)
    private Long arsenalID;

    // Identifiant de l'emplacement provenant de Cubix
    @Column(name = "emplacement_id", nullable = false)
    private Long emplacementID;

    // Catégorie de linge stockée
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategLinge categorie;

    // Quantité actuellement disponible
    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @Column(name = "date_mise_a_jour", nullable = false)
    private LocalDateTime dateMiseAJour;

    @PrePersist
    protected void onCreate() {
        if (quantite == null) {
            quantite = 0;
        }
        if (dateMiseAJour == null) {
            dateMiseAJour = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateMiseAJour = LocalDateTime.now();
    }
}
