package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Distribution;
import java.util.List;
public interface DistributionRepository extends JpaRepository<Distribution, Long> {
    List<Distribution> findByAgentId(Long agentId);
    List<Distribution> findByCollecte_Id(Long collecteId);
}
