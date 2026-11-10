package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.ProgrammeLavage;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.TypeProgramme;

import java.util.List;
import java.util.Optional;

public interface ProgrammeLavageRepository extends JpaRepository<ProgrammeLavage, Long> {
    Optional<ProgrammeLavage> findByCategoriesContaining(CategLinge categorie);
    Optional<ProgrammeLavage> findFirstByType(TypeProgramme type);
    Optional<ProgrammeLavage> findByNom(String nom);

    // Programme de lavage correspondant à une catégorie (déterministe : plus petit id)
    Optional<ProgrammeLavage> findFirstByCategoriesContainingAndTypeOrderByIdAsc(
            CategLinge categorie, TypeProgramme type);

    // Programme par défaut
    Optional<ProgrammeLavage> findFirstByParDefautTrueAndTypeOrderByIdAsc(TypeProgramme type);

    List<ProgrammeLavage> findByParDefautTrue();

}