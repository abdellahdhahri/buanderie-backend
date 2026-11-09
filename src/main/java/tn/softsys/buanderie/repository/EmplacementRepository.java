package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

public interface EmplacementRepository extends JpaRepository<Emplacement, Long> {

    List<Emplacement> findByArsenal_IdOrderByNumeroAscNomAsc(Long arsenalId);

    List<Emplacement> findByArsenal_IdAndActifTrueOrderByNumeroAscNomAsc(Long arsenalId);

    List<Emplacement> findByActifTrueOrderByArsenal_IdAscNumeroAscNomAsc();

    Optional<Emplacement> findByCodeBarre(String codeBarre);

    boolean existsByCodeBarre(String codeBarre);

    List<Emplacement> findBySource(SourceDonnee source);

    Optional<Emplacement> findBySourceAndIdExterne(SourceDonnee source, String idExterne);
}
