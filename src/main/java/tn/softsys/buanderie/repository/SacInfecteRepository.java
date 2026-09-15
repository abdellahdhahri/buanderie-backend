package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.enums.EtatSac;
import tn.softsys.buanderie.entity.SacInfecte;

import java.util.List;
import java.util.Optional;

@Repository
public interface SacInfecteRepository extends JpaRepository<SacInfecte, Long> {

    Optional<SacInfecte> findByNumeroSac(String numeroSac);

    List<SacInfecte> findByEtat(EtatSac etat);
    long countByCollecte_Id(Long collecteId);


    List<SacInfecte> findByCollecte_Id(Long collecteId);
    @Query("SELECT s FROM SacInfecte s WHERE s.personnelId = :personnelId ORDER BY s.dateCollecte DESC")

    List<SacInfecte> findByPersonnelId(@Param("personnelId") Long personnelId);

    @Query("SELECT s FROM SacInfecte s WHERE s.ecart <> 0 AND s.ecart IS NOT NULL ORDER BY s.dateVerification DESC")
    List<SacInfecte> findSacsAvecEcart();

    @Query("SELECT s FROM SacInfecte s WHERE s.etat = 'EN_LAVAGE' ORDER BY s.dateLavage ASC")
    List<SacInfecte> findSacsEnAttenteVerification();

    @Query("SELECT s FROM SacInfecte s WHERE s.etat IN ('DECLARE', 'RECEPTIONNE') ORDER BY s.dateCollecte DESC")
    List<SacInfecte> findSacsNonLaves();



}
