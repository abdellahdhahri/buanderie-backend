package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Arsenal", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les Arsenals de SQL Server")
public class Arsenal {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "NbrCol")
    private String nbrCol;

    @Column(name = "Categorie")
    private Integer categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LotArticle_ImmobilistationID")

    private Machine machine;
}





