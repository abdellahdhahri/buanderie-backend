package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;

import java.util.List;
import java.util.Optional;

@Repository
public interface LingeRepository extends JpaRepository<Linge, Long>, JpaSpecificationExecutor<Linge> {

    Optional<Linge> findByCodeBarre(String codeBarre);
    List<Linge>     findByBacId(Long bacId);
    List<Linge>     findByEtat(EtatLinge etat);
    boolean         existsByCodeBarre(String codeBarre);
    List<Linge>     findByBac_Code(String code);
    List<Linge>     findByLot_CodeLot(String codeLot);
    Linge           findByRfidTag(String tag);
    boolean         existsByRfidTag(String codeRfid);
    List<Linge>     findByRfidTagIn(List<String> rfidTags);
    List<Linge> findByEmplacement_Id(Long arsenalId);
    List<Linge> findByLot_Id(Long lotId);
    Optional<Linge> findByLegacyLotArticleImmobilisationId(
            Long legacyLotArticleImmobilisationId
    );
    List<Linge> findByLot_IdAndEtat(Long lotId, EtatLinge etat);

    @Query("SELECT l FROM Linge l WHERE l.codeBarre = :code OR l.rfidTag = :code")
    Optional<Linge> findByBarcodeOrRfid(@Param("code") String code);

    // ── Collecte chambre : linges LIVRE liés à des lits SQL Server ──────────
    List<Linge> findByLitIdSqlServerInAndEtat(List<Long> litIdSqlServerList, EtatLinge etat);

    // ── Collecte service : linges LIVRE d'un service ────────────────────────
    List<Linge> findByServiceIdAndEtat(Long serviceId, EtatLinge etat);

    // ── Collecte uniforme : linges LIVRE d'un porteur ───────────────────────
    List<Linge> findByPorteurIdAndEtat(Long porteurId, EtatLinge etat);

    // ── Distribution → affectation au lit : linges LIVRE sans lit assigné ───
    List<Linge> findByEtatAndLitIdSqlServerIsNull(EtatLinge etat);

    // ── Distribution : linges STOCKE d'un service ───────────────────────────
    List<Linge> findByServiceIdAndEtatOrderByCategorie(Long serviceId, EtatLinge etat);
    long countByServiceIdAndCategorieAndEtat(
            Long serviceId,
            CategLinge categorie,
            EtatLinge etat
    );
}
