package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Lavage;

import java.util.List;
import java.util.Optional;

public interface LavageRepository extends JpaRepository<Lavage, Long> {
    List<Lavage> findByBacId(Long bacId);
    List<Lavage> findByAgentId(Long agentId);
    List<Lavage> findByMachineId(Long machineId);
    List<Lavage> findByStatut(String statut);
    Optional<Lavage> findByBacIdAndStatut(Long bacId, String statut);
}
