
package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Entity
@Table(
        name = "reception_arsenal_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"reception_arsenal_id", "categorie"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionArsenalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Passage de réception concerné
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_arsenal_id", nullable = false)
    private ReceptionArsenal receptionArsenal;

    // Catégorie du linge reçu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategLinge categorie;

    // Quantité réellement reçue pendant ce passage
    @Column(name = "quantite_recue", nullable = false)
    private Integer quantiteRecue;
}
