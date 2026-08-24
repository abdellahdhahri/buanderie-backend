package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.RetourRemiseItem;

import java.util.Optional;

public interface RetourRemiseItemRepository
        extends JpaRepository<RetourRemiseItem, Long> {

    boolean existsByRetourRemise_IdAndLinge_Id(
            Long retourRemiseId,
            Long lingeId
    );

    boolean existsByLinge_IdAndRetourRemise_RemiseLinge_Id(
            Long lingeId,
            Long remiseLingeId
    );

    Optional<RetourRemiseItem> findByRetourRemise_IdAndLinge_Id(
            Long retourRemiseId,
            Long lingeId
    );
}