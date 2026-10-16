package tn.softsys.buanderie.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatLinge;

public interface SuiviStockRepository extends Repository<Linge, Long> {

    @Query("""
            SELECT l.serviceId,
                   l.categorie,
                   l.etat,
                   CASE WHEN l.arsenalEmplacementID IS NULL THEN 0 ELSE 1 END,
                   CASE WHEN l.litIdSqlServer IS NULL AND l.chambreIdSqlServer IS NULL THEN 0 ELSE 1 END,
                   CASE WHEN l.porteurId IS NULL THEN 0 ELSE 1 END,
                   COUNT(l)
            FROM Linge l
            WHERE l.etatImmobilisation IS NULL OR l.etatImmobilisation NOT IN :sortis
            GROUP BY l.serviceId,
                     l.categorie,
                     l.etat,
                     CASE WHEN l.arsenalEmplacementID IS NULL THEN 0 ELSE 1 END,
                     CASE WHEN l.litIdSqlServer IS NULL AND l.chambreIdSqlServer IS NULL THEN 0 ELSE 1 END,
                     CASE WHEN l.porteurId IS NULL THEN 0 ELSE 1 END
            """)
    List<Object[]> agregerParc(@Param("sortis") Collection<EtatImmobilisation> sortis);

    @Query("""
            SELECT l
            FROM Linge l
            WHERE l.etat IN :etats
              AND (l.etatImmobilisation IS NULL OR l.etatImmobilisation NOT IN :sortis)
            ORDER BY l.serviceId, l.categorie, l.codeBarre
            """)
    List<Linge> findLingesParEtats(
            @Param("etats") Collection<EtatLinge> etats,
            @Param("sortis") Collection<EtatImmobilisation> sortis
    );

    @Query("""
            SELECT l
            FROM Linge l
            WHERE l.etat IN :etats
              AND l.serviceId = :serviceId
              AND (l.etatImmobilisation IS NULL OR l.etatImmobilisation NOT IN :sortis)
            ORDER BY l.categorie, l.codeBarre
            """)
    List<Linge> findLingesParEtatsEtService(
            @Param("etats") Collection<EtatLinge> etats,
            @Param("serviceId") Long serviceId,
            @Param("sortis") Collection<EtatImmobilisation> sortis
    );
}
