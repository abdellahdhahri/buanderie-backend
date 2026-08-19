package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Entity
@Table(
        name = "remise_linge_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"remise_linge_id", "categorie"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemiseLingeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remise_linge_id", nullable = false)
    @JsonIgnore
    private RemiseLinge remiseLinge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategLinge categorie;

    @Column(nullable = false)
    private Integer quantiteCollectee;

    @Column(nullable = false)
    private Integer quantiteDisponible;

    @Column(nullable = false)
    private Integer quantiteRemise;

    @Transient
    public Integer getQuantiteRestante() {
        return Math.max(
                0,
                quantiteCollectee - quantiteRemise
        );
    }
}