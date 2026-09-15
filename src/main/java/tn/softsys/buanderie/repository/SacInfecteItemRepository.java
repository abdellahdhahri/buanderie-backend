package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.SacInfecte;
import tn.softsys.buanderie.entity.SacInfecteItem;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.List;
import java.util.Optional;

@Repository
public interface SacInfecteItemRepository extends JpaRepository<SacInfecteItem, Long> {
    List<SacInfecteItem> findBySacInfecte_Id(Long sacInfecteId);
    Optional<SacInfecteItem> findBySacInfecteAndCategorie(
            SacInfecte sacInfecte,
            CategLinge categorie
    );
}
