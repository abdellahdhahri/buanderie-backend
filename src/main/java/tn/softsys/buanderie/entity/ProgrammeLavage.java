package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.TypeProgramme;

import java.util.List;

@Entity
@Table(name = "programmes_lavage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeLavage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nom;

    @Column(name = "temperature_celsius")
    private Integer temperatureCelsius;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "vitesse_essorage_rpm")
    private Integer vitesseEssorageRpm;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeProgramme type;


    @ElementCollection(targetClass = CategLinge.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "programme_categorie",
            joinColumns = @JoinColumn(name = "programme_id")
    )
    @Column(name = "categorie")
    private List<CategLinge> categories;
}