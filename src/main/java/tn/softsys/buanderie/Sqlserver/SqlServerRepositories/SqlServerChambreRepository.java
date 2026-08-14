package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Chambre;

import java.util.Optional;

@Repository("sqlServerChambreRepository")
public interface SqlServerChambreRepository extends JpaRepository<Chambre, Long> {
    Page<Chambre> findByServiceId(Long serviceId, Pageable pageable);
    Page<Chambre> findAll(Pageable pageable);
    Optional<Chambre> findByCodeBarre(String code);
}
