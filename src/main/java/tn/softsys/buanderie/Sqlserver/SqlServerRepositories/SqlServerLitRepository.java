package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Lit;

import java.util.List;

@Repository("sqlServerLitRepository")
public interface SqlServerLitRepository extends JpaRepository<Lit, Long> {
    Page<Lit> findByChambreId(Long chambreId, Pageable pageable);
    Page<Lit> findByEtat(Integer etat, Pageable pageable);
    Page<Lit> findAll(Pageable pageable);
    List<Lit> findByChambreIdAndEtat(Long chambreId, Integer etat);

}
