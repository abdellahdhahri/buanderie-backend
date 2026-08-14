package tn.softsys.buanderie.service.Process;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Chambre;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Lit;
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
    private final SqlServerChambreRepository sqlChambreRepo;
    private final SqlServerLitRepository     sqlLitRepo;
    private final SqlServerSejourRepository  sqlSejourRepo;

    public Map<String, Object> scanChambre(String codeBarre, String matriculeAgent) {
        agentRepo.findByMatricule(matriculeAgent)
                .orElseThrow(() -> new ResourceNotFoundException("Agent","matricule",matriculeAgent));
        Chambre chambre = sqlChambreRepo.findByCodeBarre(codeBarre)
                .orElseThrow(() -> new BusinessException("Chambre introuvable : " + codeBarre));

        Set<Long> infectes = sqlSejourRepo.findLitsInfectes()
                .stream().map(Lit::getId).collect(Collectors.toSet());

        List<Lit> lits = sqlLitRepo.findByChambreId(chambre.getId(), PageRequest.of(0,50)).getContent();

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
        Agent agent = agentRepo.findByMatricule(matriculeAgent)
                .orElseThrow(() -> new ResourceNotFoundException("Agent","matricule",matriculeAgent));

        Linge linge = lingeRepo.findByCodeBarre(codeOuTag)
                .or(() -> lingeRepo.findByBarcodeOrRfid(codeOuTag))
                .orElseThrow(() -> new ResourceNotFoundException("Linge","code",codeOuTag));

        if (linge.getEtat() != EtatLinge.LIVRE)
            throw new BusinessException("Linge non disponible — état : " + linge.getEtat());

        if (linge.getLitIdSqlServer() != null)
            throw new BusinessException("Linge déjà affecté au lit " + linge.getLitIdSqlServer());

        // ✅ PIVOT RÉTABLI — ferme la boucle collecte ↔ distribution
        linge.setLitIdSqlServer(litId);
        linge.setChambreIdSqlServer(chambreId);
        linge.setStatutCollecte(null); // prêt pour prochaine collecte
        lingeRepo.save(linge);

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(EtatLinge.LIVRE)
                .etatApres(EtatLinge.LIVRE)
                .agent(agent)
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
        List<Lit> lits = sqlLitRepo.findByChambreId(chambreId, PageRequest.of(0,50)).getContent();
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
