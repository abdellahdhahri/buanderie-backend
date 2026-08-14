package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.CategMachine;
import tn.softsys.buanderie.entity.enums.TypeMachine;

import java.util.UUID;

@Entity
@Table(name = "machines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String reference;
    @Column(nullable = false, length = 50)

    @Enumerated(EnumType.STRING)

    private TypeMachine type;
    @Column(nullable = false, length = 50)
    private String marque;
    @Column(name = "categorie_Machine")
    private CategMachine categorie;
    @Column(name = "capacite_kg")
    private Double capaciteKg;
    private String code;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtatMachine etat = EtatMachine.DISPONIBLE;
    @Column(name = "localisation", length = 100)
    private String localisation;
    public enum EtatMachine {
        DISPONIBLE, EN_COURS, EN_MAINTENANCE, HORS_SERVICE
    }
    @PrePersist
    public void prePersist() {
        if (this.code == null) {
            this.code = "MCH" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }

    }

}
