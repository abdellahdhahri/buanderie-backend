package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;

@EnableJpaRepositories
public interface SqlServerPersonnelRepository extends JpaRepository<Personnel, Long> {

    Personnel findByCodeBarre(String code);
}
