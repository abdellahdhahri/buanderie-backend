package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

public interface LitRepository extends JpaRepository<Lit, Long> {

    List<Lit> findByChambreId(Long chambreId);

    @Query("SELECT l FROM Lit l WHERE l.chambre.id = :chambreId AND (l.actif IS NULL OR l.actif = true) ORDER BY l.code")
    List<Lit> findActifsByChambre(@Param("chambreId") Long chambreId);

    Lit findFirstByCodeOrderByIdAsc(String code);

    List<Lit> findBySource(SourceDonnee source);

    Optional<Lit> findBySourceAndIdExterne(SourceDonnee source, String idExterne);

    @Query("SELECT l FROM Lit l WHERE l.infecte = true AND (l.actif IS NULL OR l.actif = true)")
    List<Lit> findInfectesActifs();

    @Modifying
    @Query("UPDATE Lit l SET l.actif = true WHERE l.actif IS NULL")
    int activerNonRenseignes();

    @Modifying
    @Query("UPDATE Lit l SET l.infecte = false WHERE l.infecte IS NULL")
    int infecteParDefaut();

    @Modifying
    @Query("UPDATE Lit l SET l.source = :source WHERE l.source IS NULL")
    int definirSourceManquante(@Param("source") SourceDonnee source);
}
