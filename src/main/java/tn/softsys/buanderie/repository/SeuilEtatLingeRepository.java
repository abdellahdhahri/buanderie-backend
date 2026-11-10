package tn.softsys.buanderie.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.softsys.buanderie.entity.SeuilEtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;

public interface SeuilEtatLingeRepository extends JpaRepository<SeuilEtatLinge, Long> {

    Optional<SeuilEtatLinge> findByEtat(EtatLinge etat);
}