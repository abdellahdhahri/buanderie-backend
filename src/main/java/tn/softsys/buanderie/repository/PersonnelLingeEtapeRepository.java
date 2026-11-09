package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.PersonnelLingeEtape;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonnelLingeEtapeRepository
        extends JpaRepository<PersonnelLingeEtape, Long> {
    List<PersonnelLingeEtape> findByLingeEtapeId(Long lingeEtapeId);

    List<PersonnelLingeEtape> findByPersonnelId(Long personnelId);

    boolean existsByPersonnelIdAndLingeEtapeId(
            Long personnelId,
            Long lingeEtapeId
    );

    void deleteByPersonnelId(Long personnelId);

    void deleteByLingeEtapeId(Long lingeEtapeId);


}