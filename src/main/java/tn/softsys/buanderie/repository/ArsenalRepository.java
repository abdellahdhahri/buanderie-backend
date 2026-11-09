package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

public interface ArsenalRepository extends JpaRepository<Arsenal, Long> {

    Arsenal findFirstByServiceIdOrderByIdAsc(Long serviceId);

    default Arsenal findByServiceId(Long serviceId) {
        return findFirstByServiceIdOrderByIdAsc(serviceId);
    }

    boolean existsByCode(String code);

    Optional<Arsenal> findByCode(String code);

    List<Arsenal> findBySource(SourceDonnee source);

    Optional<Arsenal> findBySourceAndIdExterne(SourceDonnee source, String idExterne);

    @Query("SELECT a FROM Arsenal a WHERE a.actif IS NULL OR a.actif = true ORDER BY a.nom")
    List<Arsenal> findActifs();

    @Modifying
    @Query("UPDATE Arsenal a SET a.actif = true WHERE a.actif IS NULL")
    int activerNonRenseignes();

    @Modifying
    @Query("UPDATE Arsenal a SET a.source = :source WHERE a.source IS NULL")
    int definirSourceManquante(@Param("source") SourceDonnee source);
}
