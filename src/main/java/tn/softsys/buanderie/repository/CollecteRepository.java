package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.EtatCollecte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollecteRepository extends JpaRepository<Collecte, Long> {
    Optional<Collecte> findTopByServiceOrderByDateCollecteDesc(ServiceEntity service);
    Optional<Collecte> findFirstByServiceAndEtatCollecteNotOrderByDateCollecteAsc(Long serviceId, EtatCollecte etat);
    List<Collecte> findByAgent_MatriculeOrderByDateCollecteDesc(String matricule);

    List<Collecte> findByAgent_Matricule(String matricule);

    List<Collecte> findByService(Long serviceId);

    List<Collecte> findByEtatCollecte(EtatCollecte etat);

    List<Collecte> findByCodeChambre(String codeChambre);

    @Query("SELECT c FROM Collecte c WHERE c.dateCollecte BETWEEN :debut AND :fin ORDER BY c.dateCollecte DESC")
    List<Collecte> findByPeriode(@Param("debut") LocalDateTime debut,
                                 @Param("fin")   LocalDateTime fin);

}
