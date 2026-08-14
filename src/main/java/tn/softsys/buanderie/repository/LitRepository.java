package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Lit;
import java.util.List;
public interface LitRepository extends JpaRepository<Lit, Long> {
    List<Lit> findByChambreId(Long chambreId);
}
