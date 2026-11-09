package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.RemiseLingeLinge;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Repository
public interface RemiseLingeLingeRepository extends JpaRepository<RemiseLingeLinge, Long> {

    @Query("""
        SELECT r
        FROM RemiseLingeLinge r
        JOIN FETCH r.linge
        WHERE r.remiseLinge.id = :remiseLingeId
          AND r.dateRangement IS NULL
        ORDER BY r.categorie, r.dateScan
    """)
    List<RemiseLingeLinge> findARanger(@Param("remiseLingeId") Long remiseLingeId);

    Optional<RemiseLingeLinge> findByRemiseLinge_IdAndLinge_Id(Long remiseLingeId, Long lingeId);

    long countByRemiseLinge_IdAndDateRangementIsNull(Long remiseLingeId);

    // Dernier rangement en arsenal d'un linge (pour l'annuler)
    Optional<RemiseLingeLinge> findFirstByLinge_IdAndDateRangementIsNotNullOrderByDateRangementDesc(Long lingeId);

    @Query("""
        SELECT COUNT(r)
        FROM RemiseLingeLinge r
        JOIN r.remiseLinge rl
        LEFT JOIN r.collecte c
        WHERE r.categorie = :categorie
          AND (c.id = :collecteId OR (c IS NULL AND rl.collecte.id = :collecteId))
    """)
    long countRemisPourCollecte(
            @Param("collecteId") Long collecteId,
            @Param("categorie") CategLinge categorie
    );

    @Query("""
        SELECT r.categorie, COUNT(r)
        FROM RemiseLingeLinge r
        JOIN r.remiseLinge rl
        LEFT JOIN r.collecte c
        WHERE c.id = :collecteId OR (c IS NULL AND rl.collecte.id = :collecteId)
        GROUP BY r.categorie
    """)
    List<Object[]> countRemisParCategoriePourCollecte(@Param("collecteId") Long collecteId);
}
