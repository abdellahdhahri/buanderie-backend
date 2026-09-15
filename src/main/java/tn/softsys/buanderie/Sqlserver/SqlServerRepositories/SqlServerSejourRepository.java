package tn.softsys.buanderie.Sqlserver.SqlServerRepositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Lit;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Sejour;

import java.util.List;

@Repository
public interface SqlServerSejourRepository extends JpaRepository<Sejour, Long> {
    Page<Sejour> findByEtatChambreActual(
            Integer etat,
            Pageable pageable
    );
    Page<Sejour> findAll(Pageable pageable);
    @Query("""
             SELECT DISTINCT s.lit
                FROM Sejour s
                WHERE s.virus = 1
                OR s.BMR =1
                AND s.dateExit IS NULL
                AND s.lit IS NOT NULL
     
    """)
    List<Lit> findLitsInfectes();

    @Query("""
    SELECT CASE 
        WHEN COUNT(s) > 0 THEN true 
        ELSE false 
    END
    FROM Sejour s
    WHERE s.lit.id = :litId
    AND s.virus = 1
    AND s.dateExit IS NULL
""")
    boolean isLitInfecte(@Param("litId") Long litId);

}
