package tn.softsys.buanderie.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatLinge;

public interface LingeIncompletRepository extends Repository<Linge, Long> {

    @Query("""
            SELECT l.id, l.codeBarre, l.designation, l.categorie, l.etat,
                   l.serviceId, l.poids, l.dureVie
            FROM Linge l
            WHERE (l.etat IS NULL OR l.etat NOT IN :etatsExclus)
              AND (l.etatImmobilisation IS NULL OR l.etatImmobilisation NOT IN :immobilisationsExclues)
              AND (
                    l.poids IS NULL OR l.poids <= 0
                 OR l.dureVie IS NULL OR l.dureVie <= 0
                 OR l.categorie IS NULL OR l.categorie = :categorieAutre
                 OR l.serviceId IS NULL
                 OR l.designation IS NULL OR TRIM(l.designation) = ''
              )
            ORDER BY l.id
            """)
    List<Object[]> findLingesIncomplets(
            @Param("etatsExclus") Collection<EtatLinge> etatsExclus,
            @Param("immobilisationsExclues") Collection<EtatImmobilisation> immobilisationsExclues,
            @Param("categorieAutre") CategLinge categorieAutre
    );
}
