package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Personnel", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les personnels de sqlSever")

public class Personnel {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "ID")
    private Long id;

    @Column(name = "Nom")
    private String nom;

    @Column(name = "Prenom")
    private String prenom;


    @Column(name = "Fonction")

    private String fonction;

    @Column(name = "FonctionID")
    private Long fonctionId;

    @Column(name = "CodeBarre")
    private String codeBarre;
}
