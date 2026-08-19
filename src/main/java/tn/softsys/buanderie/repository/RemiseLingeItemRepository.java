package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.RemiseLinge;
import tn.softsys.buanderie.entity.RemiseLingeItem;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.List;
import java.util.Optional;

public interface RemiseLingeItemRepository
        extends JpaRepository<RemiseLingeItem, Long> {

    List<RemiseLingeItem> findByRemiseLinge(
            RemiseLinge remiseLinge
    );

    Optional<RemiseLingeItem> findByRemiseLingeAndCategorie(
            RemiseLinge remiseLinge,
            CategLinge categorie
    );

    List<RemiseLingeItem> findByRemiseLinge_Collecte_Id(
            Long collecteId
    );

    List<RemiseLingeItem> findByRemiseLinge_Collecte_IdAndCategorie(
            Long collecteId,
            CategLinge categorie
    );


    @Query("""
        SELECT COALESCE(SUM(i.quantiteRemise), 0)
        FROM RemiseLingeItem i
        WHERE i.remiseLinge.collecte.id = :collecteId
          AND i.categorie = :categorie
    """)
    Integer sumQuantiteRemiseByCollecteAndCategorie(
            @Param("collecteId") Long collecteId,
            @Param("categorie") CategLinge categorie
    );

    @Query("""
        SELECT COALESCE(SUM(i.quantiteRemise), 0)
        FROM RemiseLingeItem i
        WHERE i.remiseLinge.collecte.id = :collecteId
          AND i.categorie = :categorie
          AND i.remiseLinge.id < :remiseId
    """)
    Integer sumQuantiteRemiseByCollecteAndCategorieBeforeRemise(
            @Param("collecteId") Long collecteId,
            @Param("categorie") CategLinge categorie,
            @Param("remiseId") Long remiseId
    );
}