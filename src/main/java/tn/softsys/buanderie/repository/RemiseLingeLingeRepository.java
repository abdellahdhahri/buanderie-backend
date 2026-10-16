package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.RemiseLingeLinge;

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
}
