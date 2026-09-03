package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Article;

@Repository("sqlServerMachineRepository")
public interface SqlServerMachineRepository extends JpaRepository<Article, Long> {
    Page<Article> findAll(Pageable pageable);
    Page<Article>findByArticleFamilleId(Pageable pageable, Integer familleId);

}
