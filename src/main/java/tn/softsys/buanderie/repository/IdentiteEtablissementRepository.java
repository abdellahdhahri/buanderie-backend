package tn.softsys.buanderie.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.IdentiteEtablissement;
import tn.softsys.buanderie.entity.LogoEtablissement;

public interface IdentiteEtablissementRepository extends JpaRepository<IdentiteEtablissement, Long> {

    @Query("SELECT l FROM IdentiteEtablissement i JOIN i.logo l WHERE i.id = :id")
    Optional<LogoEtablissement> findLogo(@Param("id") Long id);

    @Query("SELECT l.version FROM IdentiteEtablissement i JOIN i.logo l WHERE i.id = :id")
    Optional<Long> findVersionDuLogo(@Param("id") Long id);
}
