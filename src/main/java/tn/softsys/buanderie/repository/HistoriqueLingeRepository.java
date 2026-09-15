package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.TypeOperation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoriqueLingeRepository extends JpaRepository<HistoriqueLinge, Long> {

    List<HistoriqueLinge> findByLingeIdOrderByDateOperationDesc(Long lingeId);

    List<HistoriqueLinge> findByLingeCodeBarreOrderByDateOperationDesc(String codeBarre);



    List<HistoriqueLinge> findByCollecte_IdOrderByDateOperationDesc(Long collecteId);

    List<HistoriqueLinge> findBySacInfecte_NumeroSacOrderByDateOperationDesc(String numeroSac);


    List<HistoriqueLinge> findByDateOperationBetweenOrderByDateOperationDesc(
            LocalDateTime debut, LocalDateTime fin);

    @Query("""
    SELECT h.personnelId,
           COUNT(h),
           MAX(h.typeOperation)
    FROM HistoriqueLinge h
    WHERE h.dateOperation >= :debutJour
      AND h.personnelId IS NOT NULL
    GROUP BY h.personnelId
    ORDER BY COUNT(h) DESC
""")
    List<Object[]> activiteAgentsAujourdhui(
            @Param("debutJour") LocalDateTime debutJour);

    // ── Linges bloqués
    // Récupère tous les historiques antérieurs au seuil
    // dont l'état final est encore en cours de traitement.
    // Le service filtre ensuite le dernier par linge en Java.
    @Query("""
        SELECT h FROM HistoriqueLinge h
        WHERE h.etatApres IN ('COLLECTE', 'RECEPTION', 'EN_TRI')
          AND h.dateOperation < :seuil
          AND h.linge IS NOT NULL
        ORDER BY h.dateOperation DESC
    """)
    List<HistoriqueLinge> findHistoriquesBloqués(@Param("seuil") LocalDateTime seuil);

    // ── Nombre d'opérations par type sur une période ──────────────────────────
    @Query("""
        SELECT COUNT(h) FROM HistoriqueLinge h
        WHERE h.typeOperation = :op
          AND h.dateOperation BETWEEN :debut AND :fin
    """)
    long countByTypeOperationAndPeriode(
            @Param("op")    TypeOperation op,
            @Param("debut") LocalDateTime debut,
            @Param("fin")   LocalDateTime fin);

    // ── Tracking chronologique d'un linge ─────────────────────────────────────
    @Query("""
        SELECT h FROM HistoriqueLinge h
        WHERE h.linge.id = :lingeId
        ORDER BY h.dateOperation ASC
    """)
    List<HistoriqueLinge> findTrackingLinge(@Param("lingeId") Long lingeId);

    @Query("""
    SELECT h
    FROM HistoriqueLinge h
    WHERE h.linge IS NOT NULL
      AND h.etatApres IS NOT NULL
      AND h.dateOperation < :seuil
    ORDER BY h.linge.id, h.dateOperation DESC
""")
    List<HistoriqueLinge> findHistoriquesAvantSeuil(
            @Param("seuil") LocalDateTime seuil);

    @Query("""
    SELECT h
    FROM HistoriqueLinge h
    JOIN FETCH h.linge l
    WHERE l.id IN :lingeIds
    ORDER BY l.id ASC, h.dateOperation DESC
""")
    List<HistoriqueLinge> findHistoriqueByLingeIds(
            @Param("lingeIds") List<Long> lingeIds
    );
}