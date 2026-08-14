package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Stockage;
import java.util.List;
public interface StockageRepository extends JpaRepository<Stockage, Long> {
    List<Stockage> findByBacId(Long bacId);
    List<Stockage> findByAgentId(Long agentId);
    List<Stockage> findByDateSortieIsNull(); // bacs encore en stock
}
