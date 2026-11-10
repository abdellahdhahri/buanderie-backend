package tn.softsys.buanderie.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.EtatLinge;

public interface LingeBloqueRepository extends Repository<Linge, Long> {


    @Query("""
            SELECT l.id, l.codeBarre, l.designation, l.categorie, l.etat,
                   l.serviceId, MAX(h.dateOperation), l.dateCreation
            FROM Linge l
            LEFT JOIN HistoriqueLinge h ON h.linge = l AND h.etatApres = l.etat
            WHERE l.etat IN :etats
            GROUP BY l.id, l.codeBarre, l.designation, l.categorie, l.etat,
                     l.serviceId, l.dateCreation
            """)
    List<Object[]> findLingesParEtats(@Param("etats") Collection<EtatLinge> etats);


    @Query("""
            SELECT l.id, l.codeBarre, l.designation, l.categorie, l.etat,
                   l.serviceId, MAX(rl.dateScan), l.dateCreation
            FROM RemiseLingeLinge rl
            JOIN rl.linge l
            WHERE l.etat = tn.softsys.buanderie.entity.enums.EtatLinge.LIVRE
              AND rl.dateRangement IS NULL
            GROUP BY l.id, l.codeBarre, l.designation, l.categorie, l.etat,
                     l.serviceId, l.dateCreation
            """)
    List<Object[]> findLingesLivresNonRanges();
}