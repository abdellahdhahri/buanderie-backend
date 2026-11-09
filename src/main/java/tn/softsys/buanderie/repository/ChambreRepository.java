package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

public interface ChambreRepository extends JpaRepository<Chambre, Long> {

    Optional<Chambre> findByCodeBarre(String codeBarre);

    boolean existsByCodeBarre(String codeBarre);

    List<Chambre> findBySource(SourceDonnee source);

    Optional<Chambre> findBySourceAndIdExterne(SourceDonnee source, String idExterne);

    @Query("SELECT c FROM Chambre c WHERE c.actif IS NULL OR c.actif = true ORDER BY c.code")
    List<Chambre> findActives();

    @Modifying
    @Query("UPDATE Chambre c SET c.actif = true WHERE c.actif IS NULL")
    int activerNonRenseignees();

    @Modifying
    @Query("UPDATE Chambre c SET c.source = :source, c.idExterne = CAST(c.id AS String) WHERE c.source IS NULL")
    int migrerChambresImportees(@Param("source") SourceDonnee source);
}
