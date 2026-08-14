package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Reception;
import java.util.List;
public interface ReceptionRepository extends JpaRepository<Reception, Long> {
    List<Reception> findByBacId(Long bacId);
    List<Reception> findByAgentId(Long agentId);
    List<Reception> findByChambreId(Long chambreId);

}
