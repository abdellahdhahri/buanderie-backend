
package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.ReceptionArsenalEmplacement;

@Repository
public interface ReceptionArsenalEmplacementRepository
        extends JpaRepository<ReceptionArsenalEmplacement, Long> {

    @Query("""
        SELECT e
        FROM ReceptionArsenalEmplacement e
        WHERE e.receptionArsenalItem.receptionArsenal.id = :receptionId
    """)
    List<ReceptionArsenalEmplacement> findByReceptionId(
            @Param("receptionId") Long receptionId
    );
}
