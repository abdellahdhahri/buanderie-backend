package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Sejour;
import java.util.List;
public interface SejourRepository extends JpaRepository<Sejour, Long> {
    List<Sejour> findByChambreId(Long chambreId);
}
