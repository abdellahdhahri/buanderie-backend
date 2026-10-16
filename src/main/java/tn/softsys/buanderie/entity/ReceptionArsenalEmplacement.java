
package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reception_arsenal_emplacements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceptionArsenalEmplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Catégorie et quantité reçues pendant ce passage
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_arsenal_item_id", nullable = false)
    private ReceptionArsenalItem receptionArsenalItem;

    // Identifiant de l'emplacement provenant de Cubix
    @Column(name = "emplacement_id", nullable = false)
    private Long emplacementID;

    // Quantité rangée dans cet emplacement
    @Column(name = "quantite_rangee", nullable = false)
    private Integer quantiteRangee;
}
