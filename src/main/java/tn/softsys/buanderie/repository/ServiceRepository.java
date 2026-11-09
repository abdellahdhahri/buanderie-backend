package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    boolean existsByNom(String nom);

    ServiceEntity findByCodeService(String code);

    List<ServiceEntity> findBySource(SourceDonnee source);

    Optional<ServiceEntity> findBySourceAndIdExterne(SourceDonnee source, String idExterne);

    List<ServiceEntity> findByActifTrueOrderByNomAsc();

    List<ServiceEntity> findAllByOrderByNomAsc();

    Optional<ServiceEntity> findFirstByReferenceIgnoreCaseOrCodeServiceIgnoreCase(String reference, String code);

    default Optional<ServiceEntity> findByReferenceIgnoreCaseOrCodeIgnoreCase(String reference, String code) {
        return findFirstByReferenceIgnoreCaseOrCodeServiceIgnoreCase(reference, code);
    }

    @Modifying
    @Query("UPDATE ServiceEntity s SET s.actif = true WHERE s.actif IS NULL")
    int activerServicesSansEtat();

    @Modifying
    @Query("UPDATE ServiceEntity s SET s.source = :source WHERE s.source IS NULL")
    int definirSourceManquante(@Param("source") SourceDonnee source);
}