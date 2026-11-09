package tn.softsys.buanderie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;

@Repository
public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    Optional<Personnel> findFirstByCodeBarreAndActifTrue(String codeBarre);

    default Personnel findByCodeBarre(String codeBarre) {
        if (codeBarre == null || codeBarre.isBlank()) {
            return null;
        }
        return findFirstByCodeBarreAndActifTrue(codeBarre.trim()).orElse(null);
    }

    List<Personnel> findByIdIn(List<Long> ids);

    List<Personnel> findByActifTrueOrderByNomAscPrenomAsc();

    List<Personnel> findAllByOrderByNomAscPrenomAsc();

    List<Personnel> findBySource(SourceDonnee source);

    Optional<Personnel> findBySourceAndIdExterne(SourceDonnee source, String idExterne);

    boolean existsByCodeBarreAndActifTrue(String codeBarre);

    boolean existsByCodeBarreAndActifTrueAndIdNot(String codeBarre, Long id);
}
