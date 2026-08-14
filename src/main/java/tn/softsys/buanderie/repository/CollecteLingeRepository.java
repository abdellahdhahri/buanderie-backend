package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.CollecteLinge;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.util.List;

public interface CollecteLingeRepository extends JpaRepository<CollecteLinge, Long> {
    List<CollecteLinge> findByCollecte_Id(Long collecteId);
    boolean existsByCollecte_IdAndLinge_Id(Long collecteId, Long lingeId);
    List<CollecteLinge> findByTypeCollecte(TypeCollecte type);

}
