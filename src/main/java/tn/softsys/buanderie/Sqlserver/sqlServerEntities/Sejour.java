package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "Sejour",schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entité représentant les sejours")
public class Sejour {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "EtatChambreActual")
    private Integer etatChambreActual;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LitID")
    private Lit lit;
    @Column(name="Virus")
    private Integer virus;
    @Column(name="DateExit")
    private Date dateExit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceID")
    private Service service;
    @Column(name = "PatientID")
    private  Long patientId;
    @Column(name = "BMR")
    private  Long BMR;

}
