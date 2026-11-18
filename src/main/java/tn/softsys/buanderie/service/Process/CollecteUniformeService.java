package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.service.management.ProfilExploitationService;

import tn.softsys.buanderie.repository.PersonnelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CollecteUniformeService {

    private final ProfilExploitationService profilExploitationService;
    private final LingeRepository lingeRepo;
    private final AgentRepository agentRepo;
    private final BacRepository bacRepo;
    private final CollecteRepository collecteRepo;
    private final CollecteLingeRepository collecteLingeRepo;
    private final HistoriqueLingeRepository historiqueRepo;
    private final PersonnelRepository personnelRepository;
    private final PersonnelReferentielService personnelReferentielService;
    public Map<String, Object> ouvrirUniforme(
            String matriculeAgent,
            String codeBadge) {


        PersonnelDto agent = personnelReferentielService.getByCode(matriculeAgent);
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Agent",
//                                "matricule",
//                                matriculeAgent
//                        )
//                );
        PersonnelDto p = personnelReferentielService.getByCode(codeBadge);

        if (p == null) {
            throw new BusinessException(
                    "Personnel introuvable : " + codeBadge
            );
        }

        Long personnelId = p.getId();




        Long fonctionId = p.getFonctionId();

        boolean estInfecte =
                profilExploitationService.circuitInfecteActif() &&
                        fonctionId != null &&
                        Set.of(59L, 56L, 41L, 20L).contains(fonctionId);




        List<Linge> uf =
                lingeRepo.findByPorteurIdAndEtat(
                        personnelId,
                        EtatLinge.LIVRE
                );




        uf.forEach(l -> {

            //  if (l.getStatutCollecte()
            //        == StatutCollecteLinge.PROPRE) {

            l.setStatutCollecte(null);
            lingeRepo.save(l);
            //   }
        });




        if (uf.isEmpty()) {

            throw new BusinessException(
                    "Aucun uniforme LIVRE pour "
                            + p.getNom()
                            + " "
                            + p.getPrenom()
            );
        }




        Collecte c =
                collecteRepo.save(
                        Collecte.builder()

                                .personnelId(agent.getId())

                                .typeCollecte(
                                        TypeCollecte.MIXTE
                                )

                                .etatCollecte(
                                        EtatCollecte.EN_TRAITEMENT
                                )

                                .nbAttendus(
                                        uf.size()
                                )

                                .numeroCollecte(
                                        "UNI-"
                                                + System.currentTimeMillis()
                                )

                                .build()
                );




        List<Map<String, Object>> ld =
                uf.stream()
                        .map(l ->
                                Map.<String, Object>of(
                                        "lingeId",
                                        l.getId(),

                                        "codeBarre",
                                        l.getCodeBarre(),

                                        "rfid",
                                        l.getRfidTag() != null
                                                ? l.getRfidTag()
                                                : "",

                                        "categorie",
                                        l.getCategorie().name(),

                                        "statut",
                                        "",

                                        "estInfecte",
                                        estInfecte
                                )
                        )
                        .collect(Collectors.toList());



        return Map.of(
                "collecteId",
                c.getId(),

                "personnelId",
                personnelId,

                "personnelNom",
                p.getNom() + " " + p.getPrenom(),

                "personnelFonction",
                p.getFonction() != null
                        ? p.getFonction()
                        : "",

                "estInfecte",
                estInfecte,

                "nbUniformes",
                uf.size(),

                "uniformes",
                ld
        );
    }

    @Transactional
    public Map<String, Object> traiterUniforme(Long collecteId, String codeUniforme, String codeBac) {
        Collecte c = findC(collecteId);
        Linge l = findL(codeUniforme);
        if (done(collecteId, l.getId())) throw new BusinessException("Déjà traité.");
        Bac bac = bacRepo.findByCode(codeBac).or(() -> bacRepo.findByRfidTag(codeBac)).orElseThrow(() -> new BusinessException("Bac introuvable : " + codeBac));
        boolean inf = bac.getTypebac() == TypeBac.INFECTE;
        if (inf) {
            profilExploitationService.exigerCircuitInfecte();
        }
        EtatLinge ne = inf ? EtatLinge.INFECTE_COLLECTE : EtatLinge.COLLECTE;
        StatutCollecteLinge st = inf ? StatutCollecteLinge.INFECTE_PRESUME : StatutCollecteLinge.BLANC;
        TypeCollecte tc = inf ? TypeCollecte.INFECTE : TypeCollecte.BLANC;
        EtatLinge av = l.getEtat();
        l.setBac(bac);
        l.setEtat(ne);
        l.setStatutCollecte(st);
        l.setTypeCollecte(tc);
        //l.setPorteurId(null);
        lingeRepo.save(l);
        collecteLingeRepo.save(CollecteLinge.builder().collecte(c).linge(l).typeCollecte(tc).numeroSacInfecte(inf ? bac.getCode() : null).dateScan(LocalDateTime.now()).build());
        historiqueRepo.save(HistoriqueLinge.builder().linge(l).etatAvant(av).etatApres(ne)
                .personnelId(c.getPersonnelId())
                .typeOperation(inf ? TypeOperation.DECLARATION_SAC_INFECTE : TypeOperation.COLLECTE).dateOperation(LocalDateTime.now()).collecte(c).observations((inf ? "INFECTE" : "BLANC") + " uniforme=" + codeUniforme + " bac=" + bac.getCode()).build());
        return Map.of("lingeId", l.getId(), "codeBarre", l.getCodeBarre(), "categorie", l.getCategorie().name(), "statut", inf ? "INFECTE" : "BLANC", "bac", bac.getCode());
    }

    @Transactional
    public Map<String, Object> declarerManquant(Long collecteId, Long lingeId) {
        Collecte c = findC(collecteId);
        Linge l = lingeRepo.findById(lingeId).orElseThrow(() -> new ResourceNotFoundException("Linge", "id", lingeId));
        EtatLinge av = l.getEtat();
        l.setStatutCollecte(StatutCollecteLinge.MANQUANT);
        lingeRepo.save(l);
        collecteLingeRepo.save(CollecteLinge.builder().collecte(c).linge(l).typeCollecte(TypeCollecte.MANQUANT).dateScan(LocalDateTime.now()).build());
        historiqueRepo.save(HistoriqueLinge.builder().linge(l).etatAvant(av).etatApres(EtatLinge.MANQUANT)
                .personnelId(c.getPersonnelId())
                .typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE).dateOperation(LocalDateTime.now()).collecte(c).observations("Uniforme manquant").build());
        return Map.of("lingeId", lingeId, "statut", "MANQUANT");
    }

    @Transactional
    public Map<String, Object> cloturer(Long collecteId) {
        Collecte c = findC(collecteId);
        List<CollecteLinge> it = collecteLingeRepo.findByCollecte_Id(collecteId);
        if (it.isEmpty()) throw new BusinessException("Aucun uniforme traité.");
        long b = cnt(it, TypeCollecte.BLANC), i = cnt(it, TypeCollecte.INFECTE), p = cnt(it, TypeCollecte.PROPRE_EN_PLACE), m = cnt(it, TypeCollecte.MANQUANT);
        c.setNbScannésBlanc((int) b);
        c.setNbPropres((int) p);
        c.setNbInfectésPrésumés((int) i);
        c.setNbManquants((int) m);
        c.setEtatCollecte(EtatCollecte.EN_TRAITEMENT);
        c.setDateFin(LocalDateTime.now());
        collecteRepo.save(c);
        return Map.of("collecteId", collecteId, "nbBlancs", b, "nbInfectes", i, "nbPropres", p, "nbManquants", m);
    }

    private Collecte findC(Long id) {
        return collecteRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Collecte", "id", id));
    }

    private Linge findL(String cd) {
        return lingeRepo.findByCodeBarre(cd).or(() -> lingeRepo.findByBarcodeOrRfid(cd)).orElseThrow(() -> new ResourceNotFoundException("Linge", "code", cd));
    }

    private boolean done(Long c, Long l) {
        return collecteLingeRepo.existsByCollecte_IdAndLinge_Id(c, l);
    }

    private long cnt(List<CollecteLinge> it, TypeCollecte t) {
        return it.stream().filter(i -> t == i.getTypeCollecte()).count();
    }
}
