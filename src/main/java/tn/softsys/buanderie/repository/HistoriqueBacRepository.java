package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.softsys.buanderie.entity.HistoriqueBac;
import tn.softsys.buanderie.entity.enums.TypeOperation;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoriqueBacRepository extends JpaRepository<HistoriqueBac, Long> {

    List<HistoriqueBac> findByBacIdOrderByDateOperationDesc(Long bacId);

    List<HistoriqueBac> findByAgentIdOrderByDateOperationDesc(Long agentId);

    @Query("SELECT h FROM HistoriqueBac h WHERE h.typeOperation = :op AND h.dateOperation BETWEEN :debut AND :fin")
    List<HistoriqueBac> findByOperationEtPeriode(
            @Param("op") TypeOperation op,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );
}
