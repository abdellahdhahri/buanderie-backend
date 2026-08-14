package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.CycleSechage;
import tn.softsys.buanderie.entity.enums.StatutCycle;
import java.util.List;

public interface CycleSechageRepository extends JpaRepository<CycleSechage, Long> {
    List<CycleSechage> findByStatut(StatutCycle statut);
    List<CycleSechage> findByMachine_IdAndStatut(Long machineId, StatutCycle statut);
}
