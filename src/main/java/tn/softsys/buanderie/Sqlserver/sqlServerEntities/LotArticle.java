package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "LotArticle", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotArticle {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "PrestataireArticleID")
    private Long prestataireArticleId;

    @Column(name = "NSerie")
    private String nSerie;

    @Column(name = "EtatActif")
    private Integer etatActif;
}