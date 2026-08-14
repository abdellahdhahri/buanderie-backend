package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "Service", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les services de SQL Server")
public class Service {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Code")
    private String code;

    @Column(name = "Description")
    private String description;

    @Column(name = "Ref")
    private String reference;
}
