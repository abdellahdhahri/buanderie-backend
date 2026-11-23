package tn.softsys.buanderie.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.EtatCollecte;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollecteRepository extends JpaRepository<Collecte, Long> {
    Optional<Collecte> findTopByServiceOrderByDateCollecteDesc(ServiceEntity service);
    Optional<Collecte> findFirstByServiceAndEtatCollecteNotOrderByDateCollecteAsc(Long serviceId, EtatCollecte etat);
    List<Collecte> findByPersonnelIdOrderByDateCollecteDesc(Long personnelId);
    List<Collecte> findByPersonnelId(Long personnelId);
    List<Collecte> findByService(Long serviceId);

    List<Collecte> findByEtatCollecte(EtatCollecte etat);

    List<Collecte> findByCodeChambre(String codeChambre);

    @Query("SELECT c FROM Collecte c WHERE c.dateCollecte BETWEEN :debut AND :fin ORDER BY c.dateCollecte DESC")
    List<Collecte> findByPeriode(@Param("debut") LocalDateTime debut,
                                 @Param("fin")   LocalDateTime fin);
    Optional<Collecte> findFirstByPersonnelIdAndEtatCollecteNotOrderByDateCollecteDesc(
            Long personnelId,
            EtatCollecte etatCollecte
    );

    List<Collecte> findByServiceAndEtatCollecteNotOrderByDateCollecteAscIdAsc(
            Long service,
            EtatCollecte etatCollecte
    );

    List<Collecte> findByPersonnelIdAndEtatCollecteNotOrderByDateCollecteAscIdAsc(
            Long personnelId,
            EtatCollecte etatCollecte
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Collecte c
        WHERE c.id = :id
    """)
    Optional<Collecte> findByIdForUpdate(
            @Param("id") Long id
    );
    List<Collecte> findByServiceInAndDateCollecteBetween(
            List<Long> serviceIds,
            LocalDateTime debut,
            LocalDateTime fin
    );

    List<Collecte> findByPersonnelIdAndDateCollecteBetween(
            Long personnelId,
            LocalDateTime debut,
            LocalDateTime fin
    );

    List<Collecte> findByTypeCollecteAndDateCollecteBetween(
            TypeCollecte typeCollecte,
            LocalDateTime debut,
            LocalDateTime fin
    );

    List<Collecte> findByEtatCollecteAndDateCollecteBetween(
            EtatCollecte etatCollecte,
            LocalDateTime debut,
            LocalDateTime fin
    );
}
