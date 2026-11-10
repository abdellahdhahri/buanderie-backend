package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatLinge;


@Entity
@Table(name = "seuils_etat_linge",
        uniqueConstraints = @UniqueConstraint(columnNames = "etat"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeuilEtatLinge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EtatLinge etat;

    @Column(name = "duree_max_heures", nullable = false)
    private Integer dureeMaxHeures;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}