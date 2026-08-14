package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.softsys.buanderie.entity.ProgrammeSechage;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.Optional;

public interface ProgrammeSechageRepository extends JpaRepository<ProgrammeSechage, Long> {

    @Query("SELECT p FROM ProgrammeSechage p WHERE :cat MEMBER OF p.categories")
    Optional<ProgrammeSechage> findByCategoriesContaining(@Param("cat") CategLinge categorie);
}
