package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.dtos.ServiceDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Service;

import java.util.Optional;

@Repository("sqlServerServiceRepository")
public interface SqlServerServiceRepository extends JpaRepository<Service, Long> {
    Page<Service> findAll(Pageable pageable);
    Service findByReference(String ref);
    Service findByCode(String code);
    Optional<Service> findByReferenceIgnoreCaseOrCodeIgnoreCase(
            String reference,
            String code
    );

}
