package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.RetourRemise;

import java.util.List;

public interface RetourRemiseRepository
        extends JpaRepository<RetourRemise, Long> {

//    List<RetourRemise> findByRemiseLinge_IdOrderByDateRetourDesc(
//            Long remiseLingeId
//    );
}