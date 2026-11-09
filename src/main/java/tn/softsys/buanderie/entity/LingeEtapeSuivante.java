package tn.softsys.buanderie.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "linge_etape_suivantes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_linge_etape_suivante", columnNames = {"etape_id", "etape_suivante_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class LingeEtapeSuivante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etape_id", nullable = false, foreignKey = @ForeignKey(name = "fk_suivante_etape"))
    private LingeEtape etape;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etape_suivante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_suivante_etape_suivante"))
    private LingeEtape etapeSuivante;
}
