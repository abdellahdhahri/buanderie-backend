package tn.softsys.buanderie.Sqlserver.sqlServerEntities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "PerstataireArticles", schema = "dbo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PerstataireArticles {
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "ArticleID")
    private Long articleId;
}