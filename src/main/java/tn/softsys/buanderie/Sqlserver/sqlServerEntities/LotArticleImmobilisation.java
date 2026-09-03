package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "LotArticle_Immobilistation", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotArticleImmobilisation {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "EtatActif")
    private Integer etatActif;


    @Column(name = "Designation")
    private String designation;}