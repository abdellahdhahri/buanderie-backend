package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Autoclave;

import java.util.List;

public interface AutoclaveRepository extends JpaRepository<Autoclave, Long> {

    List<Autoclave> findByCategorie(Integer categorie);

}