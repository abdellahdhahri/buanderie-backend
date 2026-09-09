package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "personnelLlinge_etapes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_personnel_linge_etape",
                        columnNames = {"personnel_id", "linge_etape_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonnelLingeEtape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "personnel_id", nullable = false)
    private Long personnelId;

    @Column(name = "linge_etape_id", nullable = false)
    private Long lingeEtapeId;
}