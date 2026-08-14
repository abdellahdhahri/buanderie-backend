package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Arsenal;


@EnableJpaRepositories
public interface SqlServerArsenalRepository extends JpaRepository<Arsenal,Long> {
}
