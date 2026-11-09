package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.entity.enums.NiveauRattachement;

import tn.softsys.buanderie.service.management.ProfilExploitationService;

import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.ChambreDto;
import tn.softsys.buanderie.dto.LitDto;
import tn.softsys.buanderie.service.referentiel.ChambreLitReferentielService;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service @RequiredArgsConstructor @Transactional
public class AffectationLitService {

    private final LingeRepository            lingeRepo;
    private final AgentRepository            agentRepo;
    private final HistoriqueLingeRepository  historiqueRepo;
    private final ChambreLitReferentielService chambreLitService;
    private final ProfilExploitationService profilExploitationService;
    private final PersonnelRepository sqlPersonnelRepo;

    public Map<String, Object> scanChambre(String codeBarre, String matriculeAgent) {
        agentRepo.findByMatricule(matriculeAgent)
                .orElseThrow(() -> new ResourceNotFoundException("Agent","matricule",matriculeAgent));
        ChambreDto chambre = chambreLitService.getChambreByCodeBarre(codeBarre);
        if (chambre == null) {
            throw new BusinessException("Chambre introuvable : " + codeBarre);
        }

        Set<Long> infectes = chambreLitService.getIdsLitsInfectes();

        List<LitDto> lits = chambreLitService.getLitsByChambre(chambre.getId());

        List<Map<String,Object>> litsDto = lits.stream().map(lit -> {
            // Linges déjà posés sur ce lit
            List<Linge> poses = lingeRepo.findByLitIdSqlServerInAndEtat(List.of(lit.getId()), EtatLinge.LIVRE);
            return Map.<String,Object>of(
                    "litId",      lit.getId(),
                    "codeLit",    lit.getCode() != null ? lit.getCode() : "",
                    "estInfecte", infectes.contains(lit.getId()),
                    "nbPoses",    poses.size(),
                    "lingesPoses", poses.stream().map(l -> Map.<String,Object>of(
                            "lingeId",   l.getId(),
                            "codeBarre", l.getCodeBarre(),
                            "categorie", l.getCategorie().name()
                    )).collect(Collectors.toList())
            );
        }).collect(Collectors.toList());

        // Linges LIVRE disponibles (sans lit assigné) — prêts à poser
        List<Linge> disponibles = lingeRepo.findByEtatAndLitIdSqlServerIsNull(EtatLinge.LIVRE)
                .stream()
                .filter(l -> chambre.getId().equals(l.getChambreIdSqlServer()) || l.getChambreIdSqlServer() == null)
                .collect(Collectors.toList());

        return Map.of(
                "chambreId",    chambre.getId(),
                "codeChambre",  chambre.getCode(),
                "nbLits",       lits.size(),
                "lits",         litsDto,
                "disponibles",  disponibles.stream().map(l -> Map.<String,Object>of(
                        "lingeId",   l.getId(),
                        "codeBarre", l.getCodeBarre(),
                        "categorie", l.getCategorie().name()
                )).collect(Collectors.toList())
        );
    }


    @Transactional
    public Map<String, Object> affecterLinge(Long litId, Long chambreId,
                                             String codeOuTag, String matriculeAgent) {
        profilExploitationService.exigerNiveau(NiveauRattachement.LIT);

        Personnel agent = sqlPersonnelRepo.findByCodeBarre(matriculeAgent);
//                .orElseThrow(() -> new ResourceNotFoundException("Agent","matricule",matriculeAgent));

        Linge linge = lingeRepo.findByCodeBarre(codeOuTag)
                .or(() -> lingeRepo.findByBarcodeOrRfid(codeOuTag))
                .orElseThrow(() -> new ResourceNotFoundException("Linge","code",codeOuTag));

        if (linge.getEtat() != EtatLinge.LIVRE)
            throw new BusinessException("Linge non disponible — état : " + linge.getEtat());

        if (linge.getLitIdSqlServer() != null)
            throw new BusinessException("Linge déjà affecté au lit " + linge.getLitIdSqlServer());

        linge.setLitIdSqlServer(litId);
        linge.setChambreIdSqlServer(chambreId);
        linge.setStatutCollecte(null); // prêt pour prochaine collecte
        lingeRepo.save(linge);

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(EtatLinge.LIVRE)
                .etatApres(EtatLinge.LIVRE)
                .personnelId(agent.getId())
                .typeOperation(TypeOperation.LIVRAISON)
                .dateOperation(LocalDateTime.now())
                .observations("Affecté lit=" + litId + " chambre=" + chambreId)
                .build());

        return Map.of(
                "lingeId",   linge.getId(),
                "codeBarre", linge.getCodeBarre(),
                "categorie", linge.getCategorie().name(),
                "litId",     litId,
                "chambreId", chambreId,
                "statut",    "AFFECTE"
        );
    }

    /** État actuel d'une chambre — combien de linges par lit. */
    public Map<String, Object> getEtatChambre(Long chambreId) {
        List<LitDto> lits = chambreLitService.getLitsByChambre(chambreId);
        List<Map<String,Object>> litsDto = lits.stream().map(lit -> {
            List<Linge> lg = lingeRepo.findByLitIdSqlServerInAndEtat(List.of(lit.getId()), EtatLinge.LIVRE);
            return Map.<String,Object>of("litId",lit.getId(),
                    "codeLit",lit.getCode()!=null?lit.getCode():"",
                    "nbLinges",lg.size(),
                    "linges",lg.stream().map(l->Map.<String,Object>of(
                            "lingeId",l.getId(),"codeBarre",l.getCodeBarre(),
                            "categorie",l.getCategorie().name())).collect(Collectors.toList()));
        }).collect(Collectors.toList());
        return Map.of("chambreId",chambreId,"lits",litsDto);
    }
}
