package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Sechage;
import java.util.List;
import java.util.Optional;
public interface SechageRepository extends JpaRepository<Sechage, Long> {
    List<Sechage> findByBacId(Long bacId);
    List<Sechage> findByAgentId(Long agentId);
    Optional<Sechage> findByBacIdAndStatut(Long bacId, String statut);
}
