
package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.ReceptionArsenal;

@Repository
public interface ReceptionArsenalRepository
        extends JpaRepository<ReceptionArsenal, Long> {

    List<ReceptionArsenal> findByRemiseLinge_IdOrderByDateReceptionDesc(
            Long remiseLingeId
    );
}
