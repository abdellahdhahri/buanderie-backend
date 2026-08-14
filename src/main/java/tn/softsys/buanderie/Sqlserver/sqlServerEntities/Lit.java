package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Lit", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les lits de SQL Server")
public class Lit {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Code")
    private String code;

    @Column(name = "Description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ChambreID")
    private Chambre chambre;

    @Column(name = "Etat")
    private Integer etat;
}
