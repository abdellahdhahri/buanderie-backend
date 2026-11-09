package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import tn.softsys.buanderie.entity.LingeEtapeSuivante;

public interface LingeEtapeSuivanteRepository extends JpaRepository<LingeEtapeSuivante, Long> {

    List<LingeEtapeSuivante> findByEtape_Id(Long etapeId);

    @Query("SELECT s FROM LingeEtapeSuivante s JOIN FETCH s.etape JOIN FETCH s.etapeSuivante")
    List<LingeEtapeSuivante> findAllAvecEtapes();
}
