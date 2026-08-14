package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.ProgrammeLavage;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.Optional;

public interface ProgrammeLavageRepository extends JpaRepository<ProgrammeLavage, Long> {
    Optional<ProgrammeLavage> findByCategoriesContaining(CategLinge categorie);

}
