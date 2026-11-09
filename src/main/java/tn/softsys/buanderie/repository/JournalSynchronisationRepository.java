package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.Referentiel;

@Repository
public interface JournalSynchronisationRepository extends JpaRepository<JournalSynchronisation, Long> {

    List<JournalSynchronisation> findTop50ByReferentielOrderByDateDebutDesc(Referentiel referentiel);

    List<JournalSynchronisation> findTop50ByOrderByDateDebutDesc();
}
