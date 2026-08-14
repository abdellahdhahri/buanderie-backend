package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Reclamation;
import java.util.List;
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

}
