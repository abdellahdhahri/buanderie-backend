package tn.softsys.buanderie.service.Process;

import tn.softsys.buanderie.repository.ServiceRepository;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.dto.ChambreDto;
import tn.softsys.buanderie.dto.LitDto;
import tn.softsys.buanderie.service.referentiel.ChambreLitReferentielService;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.service.management.ParcoursService;
import tn.softsys.buanderie.dto.Livraison.DistributionRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private final DistributionRepository       distributionRepo;
    private final DistributionItemRepository   distributionItemRepo;
    private final DistributionLingeRepository  distributionLingeRepo;
    private final CollecteRepository           collecteRepo;
    private final CollecteLingeRepository      collecteLingeRepo;
    private final SacInfecteRepository         sacInfecteRepo;
    private final LingeRepository              lingeRepo;
    private final AgentRepository              agentRepo;
    private final HistoriqueLingeRepository    historiqueRepo;

    private final ChambreLitReferentielService chambreLitService;
    private final ParcoursService parcoursService;
    private final ServiceRepository   sqlServiceRepo;
    private final PersonnelRepository sqlPersonnelRepo;



    public Map<String, Object> scanChambrePourDistribution(
            String codeBarre, String matriculeAgent) {

        if (sqlPersonnelRepo.findByCodeBarre(matriculeAgent) == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        ChambreDto chambre = chambreLitService.getChambreByCodeBarre(codeBarre);
        if (chambre == null) {
            throw new BusinessException("Chambre introuvable : " + codeBarre);
        }

        Set<Long> litsInfectes = chambreLitService.getIdsLitsInfectes();

        List<LitDto> lits = chambreLitService.getLitsByChambre(chambre.getId());

        List<Map<String, Object>> litsDto = lits.stream().map(lit -> {
            List<Linge> poses = lingeRepo
                    .findByLitIdSqlServerInAndEtat(List.of(lit.getId()), EtatLinge.LIVRE);
            boolean litVide = poses.isEmpty();
            return Map.<String, Object>of(
                    "litId",         lit.getId(),
                    "codeLit",       lit.getCode() != null ? lit.getCode() : "",
                    "estInfecte",    litsInfectes.contains(lit.getId()),
                    "nbLingesPoses", poses.size(),
                    "litVide",       litVide,
                    "lingesPoses",   poses.stream().map(l -> Map.<String, Object>of(
                            "lingeId",   l.getId(),
                            "codeBarre", l.getCodeBarre(),
                            "categorie", l.getCategorie().name()
                    )).collect(Collectors.toList())
            );
        }).collect(Collectors.toList());

        List<Linge> disponibles = lingesDisponibles().stream()
                .filter(l -> l.getChambreIdSqlServer() == null
                        || l.getChambreIdSqlServer().equals(chambre.getId()))
                .collect(Collectors.toList());

        return Map.of(
                "chambreId",   chambre.getId(),
                "codeChambre", chambre.getCode(),
                "nbLits",      lits.size(),
                "lits",        litsDto,
                "disponibles", disponibles.stream().map(l -> Map.<String, Object>of(
                        "lingeId",   l.getId(),
                        "codeBarre", l.getCodeBarre(),
                        "categorie", l.getCategorie().name()
                )).collect(Collectors.toList())
        );
    }

    @Transactional
    public Map<String, Object> distribuerSurLit(Long chambreId,
                                                Long litId,
                                                String codeOuTag,
                                                String matriculeAgent) {
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        Linge linge = findLinge(codeOuTag);

        if (!etatsDisponibles().contains(linge.getEtat()))
            throw new BusinessException(
                    "Linge non disponible — état : " + linge.getEtat()
                            + " (attendu : " + libelleEtatsDisponibles() + ")");

        if (linge.getLitIdSqlServer() != null)
            throw new BusinessException(
                    "Linge déjà affecté au lit " + linge.getLitIdSqlServer());

        Long serviceId = chambreLitService.findChambreById(chambreId)
                .map(ChambreDto::getServiceId)
                .orElse(null);

        EtatLinge ancien = linge.getEtat();

        linge.setLitIdSqlServer(litId);
        linge.setChambreIdSqlServer(chambreId);
        linge.setServiceId(serviceId);
        linge.setPorteurId(null);
        linge.setPorteurNom(null);
        linge.setEtat(EtatLinge.LIVRE);
        linge.setStatutCollecte(null);
        linge.setArsenalEmplacementID(null);
        linge.setEmplacement(null);

        lingeRepo.save(linge);

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(ancien)
                .etatApres(EtatLinge.LIVRE)
                .personnelId(agent.getId())
                .typeOperation(TypeOperation.LIVRAISON)
                .dateOperation(LocalDateTime.now())
                .observations("Distribué → lit=" + litId
                        + " chambre=" + chambreId
                        + " service=" + serviceId)
                .build());

        return Map.of(
                "lingeId",   linge.getId(),
                "codeBarre", linge.getCodeBarre(),
                "categorie", linge.getCategorie().name(),
                "litId",     litId,
                "chambreId", chambreId,
                "serviceId", serviceId != null ? serviceId : 0L,
                "statut",    "LIVRE"
        );
    }



    public Map<String, Object> getLingesStockePourService(Long serviceId,
                                                          String matriculeAgent) {
        agentRepo.findByMatricule(matriculeAgent)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agent", "matricule", matriculeAgent));

        ServiceEntity svc =
                sqlServiceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Service", "id", serviceId));

        List<Linge> disponibles = lingesDisponibles().stream()
                .filter(l -> serviceId.equals(l.getServiceId()) || l.getServiceId() == null)
                .collect(Collectors.toList());

        List<Linge> enPlace = lingeRepo.findByServiceIdAndEtat(
                serviceId, EtatLinge.LIVRE);

        return Map.of(
                "serviceId",     serviceId,
                "serviceCode",   svc.getCode(),
                "nbDisponibles", disponibles.size(),
                "nbEnPlace",     enPlace.size(),
                "disponibles",   disponibles.stream().map(l -> Map.<String, Object>of(
                        "lingeId",   l.getId(),
                        "codeBarre", l.getCodeBarre(),
                        "categorie", l.getCategorie().name()
                )).collect(Collectors.toList()),
                "enPlace", enPlace.stream().map(l -> Map.<String, Object>of(
                        "lingeId",   l.getId(),
                        "codeBarre", l.getCodeBarre(),
                        "categorie", l.getCategorie().name()
                )).collect(Collectors.toList())
        );
    }

    @Transactional
    public Map<String, Object> distribuerAuService(Long serviceId,
                                                   String codeOuTag,
                                                   String matriculeAgent) {
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        Linge linge = findLinge(codeOuTag);

        if (!etatsDisponibles().contains(linge.getEtat()))
            throw new BusinessException(
                    "Linge non disponible — état : " + linge.getEtat());

        EtatLinge ancien = linge.getEtat();

        linge.setServiceId(serviceId);
        linge.setLitIdSqlServer(null);
        linge.setChambreIdSqlServer(null);
        linge.setPorteurId(null);
        linge.setPorteurNom(null);
        linge.setEtat(EtatLinge.LIVRE);
        linge.setStatutCollecte(null);
        linge.setEmplacement(null);

        lingeRepo.save(linge);

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(ancien).etatApres(EtatLinge.LIVRE)
                .personnelId(agent.getId())
                .typeOperation(TypeOperation.LIVRAISON)
                .dateOperation(LocalDateTime.now())
                .observations("Distribué → service=" + serviceId)
                .build());

        return Map.of(
                "lingeId",   linge.getId(),
                "codeBarre", linge.getCodeBarre(),
                "categorie", linge.getCategorie().name(),
                "serviceId", serviceId,
                "statut",    "LIVRE"
        );
    }



    public Map<String, Object> getUniformesPourPersonnel(String codeBadge,
                                                         String matriculeAgent) {
        if (sqlPersonnelRepo.findByCodeBarre(matriculeAgent) == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        Personnel p = sqlPersonnelRepo.findByCodeBarre(codeBadge);
        if (p == null)
            throw new BusinessException("Personnel introuvable : " + codeBadge);

        List<Linge> enPossession = lingeRepo.findByPorteurIdAndEtat(
                p.getId(), EtatLinge.LIVRE);

        List<Linge> disponibles = lingesDisponibles().stream()
                .filter(l -> CategLinge.UNIFORME == l.getCategorie()
                        && (l.getPorteurId() == null || l.getPorteurId().equals(p.getId())))
                .collect(Collectors.toList());

        return Map.of(
                "personnelId",       p.getId(),
                "personnelNom",      p.getNom() + " " + p.getPrenom(),
                "personnelFonction", p.getFonction() != null ? p.getFonction() : "",
                "nbEnPossession",    enPossession.size(),
                "nbDisponibles",     disponibles.size(),
                "enPossession", enPossession.stream().map(l -> Map.<String, Object>of(
                        "lingeId",   l.getId(),
                        "codeBarre", l.getCodeBarre(),
                        "categorie", l.getCategorie().name()
                )).collect(Collectors.toList()),
                "disponibles", disponibles.stream().map(l -> Map.<String, Object>of(
                        "lingeId",   l.getId(),
                        "codeBarre", l.getCodeBarre(),
                        "categorie", l.getCategorie().name()
                )).collect(Collectors.toList())
        );
    }

    @Transactional
    public Map<String, Object> distribuerUniforme(Long porteurId,
                                                  String porteurNom,
                                                  String codeOuTag,
                                                  String matriculeAgent) {
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }

        Linge linge = findLinge(codeOuTag);

        if (!etatsDisponibles().contains(linge.getEtat()))
            throw new BusinessException(
                    "Linge non disponible — état : " + linge.getEtat());

        EtatLinge ancien = linge.getEtat();

        linge.setPorteurId(porteurId);
        linge.setPorteurNom(porteurNom);
        linge.setServiceId(null);
        linge.setLitIdSqlServer(null);
        linge.setChambreIdSqlServer(null);
        linge.setEtat(EtatLinge.LIVRE);
        linge.setStatutCollecte(null);
        linge.setEmplacement(null);
        lingeRepo.save(linge);

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(ancien).etatApres(EtatLinge.LIVRE)
                .personnelId(agent.getId())
                .typeOperation(TypeOperation.LIVRAISON)
                .dateOperation(LocalDateTime.now())
                .observations("Uniforme → porteur=" + porteurId + " " + porteurNom)
                .build());

        return Map.of(
                "lingeId",    linge.getId(),
                "codeBarre",  linge.getCodeBarre(),
                "categorie",  linge.getCategorie().name(),
                "porteurId",  porteurId,
                "porteurNom", porteurNom,
                "statut",     "LIVRE"
        );
    }



    @Transactional
    public Distribution creerDistribution(DistributionRequest request) {
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(request.getMatriculeAgent());
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", request.getMatriculeAgent());
        }

        ServiceEntity service =
                sqlServiceRepo.findById(request.getServiceId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Service", "id", request.getServiceId()));

        Collecte collecte = collecteRepo
                .findFirstByServiceAndEtatCollecteNotOrderByDateCollecteAsc(
                        service.getId(), EtatCollecte.CLOTUREE)
                .orElseThrow(() -> new RuntimeException(
                        "Aucune collecte active pour ce service."));

        return distributionRepo.save(Distribution.builder()
                .personnelId(agent.getId())
                .serviceId(service.getId())
                .collecte(collecte)
                .dateDistribution(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void scannerLinge(Long distributionId, String codeOuTag,
                             Long litIdSqlServer, Long chambreIdSqlServer) {
        Distribution dist = distributionRepo.findById(distributionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Distribution", "id", distributionId));

        Linge linge = findLinge(codeOuTag);

        // serviceId transmis depuis la distribution
        Long serviceId = dist.getServiceId();

        EtatLinge ancien = linge.getEtat();

        linge.setEtat(EtatLinge.LIVRE);
        linge.setStatutCollecte(null);
        linge.setServiceId(serviceId);

        if (litIdSqlServer != null) {
            linge.setLitIdSqlServer(litIdSqlServer);
            linge.setChambreIdSqlServer(chambreIdSqlServer);
        }
        lingeRepo.save(linge);

        distributionLingeRepo.save(DistributionLinge.builder()
                .distribution(dist)
                .linge(linge)
                .dateScan(LocalDateTime.now())
                .build());

        historiqueRepo.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(ancien).etatApres(EtatLinge.LIVRE)
                .personnelId(dist.getPersonnelId())
                .typeOperation(TypeOperation.LIVRAISON)
                .dateOperation(LocalDateTime.now())
                .observations("Distribution #" + distributionId
                        + " service=" + serviceId)
                .build());
    }

    public void scannerLinge(Long distributionId, String codeOuTag) {
        scannerLinge(distributionId, codeOuTag, null, null);
    }

    public Distribution getById(Long id) {
        return distributionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Distribution", "id", id));
    }

    public List<Map<String, Object>> getComparatif(Long id) {
        Distribution dist = distributionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Distribution", "id", id));

        List<DistributionLinge> livres = distributionLingeRepo.findByDistribution_Id(id);

        Map<String, Long> parCategorie = livres.stream()
                .filter(dl -> dl.getLinge() != null)
                .collect(Collectors.groupingBy(
                        dl -> dl.getLinge().getCategorie().name(),
                        Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();

        if (dist.getCollecte() != null) {
            List<CollecteLinge> collecteLinges = collecteLingeRepo
                    .findByCollecte_Id(dist.getCollecte().getId());

            Map<String, Long> collecteParCat = collecteLinges.stream()
                    .filter(cl -> cl.getLinge() != null)
                    .collect(Collectors.groupingBy(
                            cl -> cl.getLinge().getCategorie().name(),
                            Collectors.counting()));

            Set<String> toutes = new HashSet<>();
            toutes.addAll(collecteParCat.keySet());
            toutes.addAll(parCategorie.keySet());

            for (String categorie : toutes) {
                long collecte = collecteParCat.getOrDefault(categorie, 0L);
                long livre    = parCategorie.getOrDefault(categorie, 0L);
                result.add(Map.of(
                        "categorie",         categorie,
                        "quantiteCollectee", collecte,
                        "quantiteDemandee",  collecte,
                        "quantiteLivree",    livre
                ));
            }
        } else {
            parCategorie.forEach((cat, cnt) ->
                    result.add(Map.of(
                            "categorie",         cat,
                            "quantiteCollectee", 0L,
                            "quantiteDemandee",  cnt,
                            "quantiteLivree",    cnt
                    ))
            );
        }

        result.sort(Comparator.comparing(m -> (String) m.get("categorie")));
        return result;
    }



    private Set<EtatLinge> etatsDisponibles() {
        Set<EtatLinge> etats = parcoursService.etatsAcceptes(CodeEtape.DISTRIBUTION);
        return etats.isEmpty() ? Set.of(EtatLinge.STOCKE) : etats;
    }

    private String libelleEtatsDisponibles() {
        return String.join(", ", etatsDisponibles().stream().map(Enum::name).toList());
    }

    private List<Linge> lingesDisponibles() {
        List<Linge> resultat = new ArrayList<>();
        for (EtatLinge etat : etatsDisponibles()) {
            resultat.addAll(lingeRepo.findByEtat(etat));
        }
        return resultat;
    }

    private Linge findLinge(String code) {
        return lingeRepo.findByCodeBarre(code)
                .or(() -> lingeRepo.findByBarcodeOrRfid(code))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Linge", "code", code));
    }
}