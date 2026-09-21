package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "Personnel")
@Getter
@Setter
public class AgentSql {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "Nom")
    private String nom;

    @Column(name = "Prenom")
    private String prenom;

    @Column(name = "NomPrenom")
    private String nomPrenom;

    @Column(name = "Matricule")
    private String matricule;

    @Column(name = "CodeBarre")
    private String codeBarre;

    @Column(name = "FonctionID")
    private Long fonctionId;

    @Column(name = "FonctionCode")
    private String fonctionCode;

    @Column(name = "ServiceID")
    private Long serviceId;

    @Column(name = "ServiceCode")
    private String serviceCode;

    @Column(name = "PointeuseActif")
    private Boolean pointeuseActif;

    @Column(name = "Administrateur")
    private Boolean administrateur;

    @Column(name = "Visible")
    private Boolean visible;
}