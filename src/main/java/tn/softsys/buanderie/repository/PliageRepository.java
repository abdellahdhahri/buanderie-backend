package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Pliage;
import java.util.List;
public interface PliageRepository extends JpaRepository<Pliage, Long> {
    List<Pliage> findByBacId(Long bacId);
    List<Pliage> findByAgentId(Long agentId);
}
