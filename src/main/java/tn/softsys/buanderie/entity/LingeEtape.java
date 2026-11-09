package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.Zone;

@Entity
@Table(
        name = "linge_etapes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_linge_etape_code_barre",
                        columnNames = "code_barre"
                ),
                @UniqueConstraint(
                        name = "uk_linge_etape_ordre",
                        columnNames = "ordre"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LingeEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "code_barre", nullable = false, unique = true)
    private String codeBarre;

    @Column(nullable = false)
    private String categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zone zone;

    @Column(nullable = false, unique = true)
    private Integer ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 30, unique = true)
    private CodeEtape code;

    @Column(name = "actif", nullable = false, columnDefinition = "boolean default true")
    private Boolean actif = true;

    @Column(name = "obligatoire", nullable = false, columnDefinition = "boolean default false")
    private Boolean obligatoire = false;

    @Column(name = "privilege_requis", length = 50)
    private String privilegeRequis;
}