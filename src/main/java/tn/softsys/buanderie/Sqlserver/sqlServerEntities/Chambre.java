package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Chambre", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les chambres de SQL Server")
public class Chambre {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Code")
    private String code;

    @Column(name = "CodeBarre")
    private String codeBarre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceID")
    private Service service;
}
