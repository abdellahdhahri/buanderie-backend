package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "ArticleFamille", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleFamille {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Nom")
    private String nom;

    @Column(name = "Linge")
    private Integer linge;
}