package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Entity
@Table(name = "sac_infecte_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SacInfecteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sac_infecte_id", nullable = false)
    private SacInfecte sacInfecte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategLinge categorie;

    @Column(name = "quantite_declaree", nullable = false)
    private Integer quantiteDeclaree;

    @Column(name = "quantite_comptee")
    private Integer quantiteComptee;

    @Column(name = "ecart")
    private Integer ecart;
}
