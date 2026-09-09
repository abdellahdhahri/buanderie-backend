package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.Query;
import tn.softsys.buanderie.entity.LingeEtape;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.enums.Zone;

import java.util.List;
import java.util.Optional;

public interface LingeEtapeRepository extends JpaRepository<LingeEtape, Long> {

    Optional<LingeEtape> findByCodeBarre(String codeBarre);
    List<LingeEtape> findAllByOrderByOrdreAsc();

    boolean existsByCodeBarre(String codeBarre);

    List<LingeEtape> findByZone(Zone zone);

    List<LingeEtape> findByCategorie(String categorie);

    List<LingeEtape> findByZoneAndCategorie(String zone, String categorie);

    @Query("SELECT MAX(l.ordre) FROM LingeEtape l")
    Integer findMaxOrdre();
}