package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

@Entity
@Table(name = "collecte_linges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollecteLinge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "collecte_id")
    @JsonIgnore
    private Collecte collecte;

    @ManyToOne
    @JoinColumn(name = "linge_id")
    private Linge linge;

    private LocalDateTime dateScan;


    @Enumerated(EnumType.STRING)
    @Column(name = "type_collecte", length = 20)
    private TypeCollecte typeCollecte;

    /** Numéro du sac infecté d'origine (pour les linges infectés) */
    @Column(name = "numero_sac_infecte", length = 30)
    private String numeroSacInfecte;

    @PrePersist
    public void prePersist() {
        dateScan = LocalDateTime.now();
    }
}
