
package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.StockArsenal;
import tn.softsys.buanderie.entity.enums.CategLinge;

@Repository
public interface StockArsenalRepository
        extends JpaRepository<StockArsenal, Long> {

    @Query("""
        SELECT s
        FROM StockArsenal s
        WHERE s.arsenalID = :arsenalID
          AND s.emplacementID = :emplacementID
          AND s.categorie = :categorie
    """)
    Optional<StockArsenal> rechercherStock(
            @Param("arsenalID") Long arsenalID,
            @Param("emplacementID") Long emplacementID,
            @Param("categorie") CategLinge categorie
    );

    @Query("""
        SELECT s
        FROM StockArsenal s
        WHERE s.arsenalID = :arsenalID
        ORDER BY s.emplacementID, s.categorie
    """)
    List<StockArsenal> findByArsenalID(
            @Param("arsenalID") Long arsenalID
    );

    @Query("""
        SELECT s
        FROM StockArsenal s
        WHERE s.arsenalID = :arsenalID
          AND s.emplacementID = :emplacementID
        ORDER BY s.categorie
    """)
    List<StockArsenal> findByArsenalAndEmplacement(
            @Param("arsenalID") Long arsenalID,
            @Param("emplacementID") Long emplacementID
    );
}
