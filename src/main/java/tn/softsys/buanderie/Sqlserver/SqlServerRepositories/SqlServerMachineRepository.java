package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Machine;

@Repository("sqlServerMachineRepository")
public interface SqlServerMachineRepository extends JpaRepository<Machine, Long> {
    Page<Machine> findAll(Pageable pageable);
    Page<Machine>findByArticleFamilleId(Pageable pageable,Integer familleId);

}
