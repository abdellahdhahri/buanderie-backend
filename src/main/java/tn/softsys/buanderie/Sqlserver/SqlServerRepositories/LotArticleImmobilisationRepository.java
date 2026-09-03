package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.LotArticleImmobilisation;

public interface LotArticleImmobilisationRepository
        extends JpaRepository<LotArticleImmobilisation, Long> {
}