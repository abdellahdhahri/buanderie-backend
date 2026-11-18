package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.List;

@Entity
@Table(name = "programmes_sechage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeSechage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nom;              // COTON_CHAUD, DELICAT_FROID...

    @Column(name = "temperature_celsius")
    private Integer temperatureCelsius;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "niveau_humidite")
    private Integer niveauHumidite;  // % humidité résiduelle cible

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "groupe_lavage", length = 10)
    private tn.softsys.buanderie.entity.enums.GroupeLavage groupeLavage;

    @ElementCollection(targetClass = CategLinge.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "programme_sechage_categorie",
            joinColumns = @JoinColumn(name = "programme_id")
    )
    @Column(name = "categorie")
    private List<CategLinge> categories;
}
