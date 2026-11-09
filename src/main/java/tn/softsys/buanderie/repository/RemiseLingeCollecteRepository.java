package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.RemiseLinge;
import tn.softsys.buanderie.entity.RemiseLingeCollecte;

@Repository
public interface RemiseLingeCollecteRepository extends JpaRepository<RemiseLingeCollecte, Long> {

    @Query("""
        SELECT rc.collecte
        FROM RemiseLingeCollecte rc
        WHERE rc.remiseLinge.id = :remiseLingeId
        ORDER BY rc.collecte.dateCollecte ASC, rc.collecte.id ASC
    """)
    List<Collecte> findCollectesDeRemise(@Param("remiseLingeId") Long remiseLingeId);

    @Query("""
        SELECT rc.remiseLinge
        FROM RemiseLingeCollecte rc
        WHERE rc.collecte.id = :collecteId
        ORDER BY rc.remiseLinge.dateLivraison ASC
    """)
    List<RemiseLinge> findRemisesDeCollecte(@Param("collecteId") Long collecteId);
}
