package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "Article", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les machines / immobilisations de SQL Server")
public class Machine {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Designation")
    private String designation;
    @Column(name = "CodeBarre")
    private String codeBarre;

    @Column(name = "ArticleFamilleID")
    private Long articleFamilleId;

    @Column(name = "Marque")
    private String marque;
    @Column(name = "Code")
    private String code;

}
