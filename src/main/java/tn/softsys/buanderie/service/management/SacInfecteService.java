package tn.softsys.buanderie.service.management;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.dto.SacsInfectes.AffectationLingesPropresRequest;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteDTO;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteItemRequest;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteRequest;
import tn.softsys.buanderie.dto.VerificationLavageRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.entity.CollecteLinge;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SacInfecteService {


    private final SacInfecteRepository sacInfecteRepository;
    private final SacInfecteItemRepository sacInfecteItemRepository;
    private final AgentRepository agentRepository;
    private final SqlServerServiceRepository serviceRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final CollecteLingeRepository collecteLingeRepository;
    private final CollecteRepository collecteRepository;
    private final MachineRepository      machineRepository;
    private final CycleLavageRepository  cycleLavageRepository;
    private final CycleLavageItemRepository cycleLavageItemRepository;



    public SacInfecte creerSac(SacInfecteDTO sacInfecteDTO){
       /* if (sacInfecteRepository.existBynumero_sac(sacInfecteDTO.getNumeroSac())){
            throw new BusinessException("sac avec ce numero deja existe '"+ sacInfecteDTO.getNumeroSac());
        }*/
        SacInfecte sac = SacInfecte.builder ()
        //.agentCollecte(sacInfecteDTO.getA())
        //.agentLavage(sacInfecteDTO.getAgentLavageNom())
        .dateReception(sacInfecteDTO.getDateReception())
        .chambre(sacInfecteDTO.getChambre())
        .etat(EtatSac.DECLARE)
        .observations("sac crée ")
        .quantiteDeclaree(0)
        .build();

        return  sacInfecteRepository.save(sac);

    }
    //  DECLARATION appelé depuis CollecteService avec la collecte parente
    public SacInfecteDTO declarerSacInfecte(SacInfecteRequest request, Collecte collecte) {

        if (request.getItems() == null || request.getItems().isEmpty())
            throw new BusinessException("Impossible de déclarer un sac vide.");
        Agent agent = agentRepository.findByMatricule(request.getMatriculeAgent())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "matricule", request.getMatriculeAgent()));

        Optional<tn.softsys.buanderie.Sqlserver.sqlServerEntities.Service> service = serviceRepository.findById(request.getServiceId());
               // .orElseThrow(() -> new BusinessException("Service introuvable : " + request.getServiceId()));

        int totalDeclare = request.getItems().stream()
                .mapToInt(SacInfecteItemRequest::getQuantiteDeclaree).sum();
        if (totalDeclare <= 0)
            throw new BusinessException("La quantité totale déclarée doit être > 0.");
        SacInfecte sac = SacInfecte.builder()
                .collecte(collecte)
                .agentCollecte(agent)
                .serviceId(service.get().getId())
                .chambre(request.getChambre())
                .quantiteDeclaree(totalDeclare)
                .etat(EtatSac.DECLARE)
                .observations(request.getObservations())
                .build();
        sac = sacInfecteRepository.save(sac);

        List<SacInfecteItem> items = new ArrayList<>();
        for (SacInfecteItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantiteDeclaree() == null || itemReq.getQuantiteDeclaree() <= 0)
                throw new BusinessException("Quantité invalide pour : " + itemReq.getCategorie());
            items.add(SacInfecteItem.builder()
                    .sacInfecte(sac)
                    .categorie(itemReq.getCategorie())
                    .quantiteDeclaree(itemReq.getQuantiteDeclaree())
                    .build());
        }
        sacInfecteItemRepository.saveAll(items);
        sac.setItems(items);
        return toDTO(sac);
    }
    // Appelé directement depuis le controller (sans collecte parente)
    public SacInfecteDTO declarerSacInfecte(SacInfecteRequest request) {
        return declarerSacInfecte(request, null);
    }
    //  RÉCEPTION
    public SacInfecteDTO recevoirSacInfecte(String numeroSac, String matriculeAgent) {
        SacInfecte sac = sacInfecteRepository.findByNumeroSac(numeroSac)
                .orElseThrow(() -> new ResourceNotFoundException("SacInfecte", "numeroSac", numeroSac));
        if (sac.getEtat() != EtatSac.DECLARE)
            throw new BusinessException("Le sac " + numeroSac + " est déjà en état : " + sac.getEtat());

        Agent agent = agentRepository.findByMatricule(matriculeAgent)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "matricule", matriculeAgent));

        sac.setEtat(EtatSac.RECEPTIONNE);
        sac.setDateReception(LocalDateTime.now());
        sac.setAgentReception(agent);
        sacInfecteRepository.save(sac);
        return toDTO(sac);
    }

    //  MISE EN LAVAGE

    public SacInfecteDTO mettreEnLavage(String numeroSac,
                                        String matriculeAgent,
                                        String codeMachine) {

        SacInfecte sac = sacInfecteRepository.findByNumeroSac(numeroSac)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SacInfecte", "numeroSac", numeroSac));

        if (sac.getEtat() != EtatSac.RECEPTIONNE)
            throw new BusinessException(
                    "Le sac doit être RECEPTIONNE. État actuel : " + sac.getEtat());

        Agent agent = agentRepository.findByMatricule(matriculeAgent)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agent", "matricule", matriculeAgent));

        Machine machine = machineRepository.findByCode(codeMachine);
        if (machine == null)
            throw new ResourceNotFoundException("Machine", "code", codeMachine);

        if (machine.getEtat() != Machine.EtatMachine.DISPONIBLE)
            throw new BusinessException(
                    "Machine " + codeMachine + " indisponible.");

        // Créer le cycle de lavage dédié au sac infecté
        CycleLavage cycle = CycleLavage.builder()
                .machine(machine)
                .agent(agent)
                .dateDebut(LocalDateTime.now())
                .statut(StatutCycle.EN_COURS)
                .poidsCharge(0.0)
                .build();
        cycle = cycleLavageRepository.save(cycle);

        // Créer l'item lié au sac (TypeSource.SAC_INFECTE)
        CycleLavageItem item = CycleLavageItem.builder()
                .cycleLavage(cycle)
                .typeSource(TypeSource.SAC_INFECTE)
                .sourceId(sac.getId())
                .quantiteChargee(sac.getQuantiteDeclaree())
                .build();
        cycleLavageItemRepository.save(item);

        // Mettre la machine EN_COURS
        machine.setEtat(Machine.EtatMachine.EN_COURS);
        machineRepository.save(machine);

        // Mettre à jour le sac
        sac.setEtat(EtatSac.EN_LAVAGE);
        sac.setDateLavage(LocalDateTime.now());
        sac.setAgentLavage(agent);
        sac.setCycleLavage(cycle);
        sacInfecteRepository.save(sac);

        return toDTO(sac);
    }
    //  VÉRIFICATION POST-LAVAGE
    public SacInfecteDTO verifierApresLavage(VerificationLavageRequest request) {
        SacInfecte sac = sacInfecteRepository.findByNumeroSac(request.getNumeroSac())
                .orElseThrow(() -> new ResourceNotFoundException("SacInfecte", "numeroSac", request.getNumeroSac()));
        if (sac.getEtat() != EtatSac.EN_LAVAGE)
            throw new BusinessException("Le sac doit être EN_LAVAGE. État actuel : " + sac.getEtat());
        Agent agent = agentRepository.findByMatricule(request.getMatriculeAgent())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "matricule", request.getMatriculeAgent()));
        int totalCompte = request.getItemsComptes().stream()
                .mapToInt(VerificationLavageRequest.VerificationItemRequest::getQuantiteComptee).sum();
        List<SacInfecteItem> items = sac.getItems();
        for (VerificationLavageRequest.VerificationItemRequest comptage : request.getItemsComptes()) {
            items.stream().filter(i -> i.getCategorie() == comptage.getCategorie()).findFirst()
                    .ifPresent(i -> {
                        i.setQuantiteComptee(comptage.getQuantiteComptee());
                        i.setEcart(comptage.getQuantiteComptee() - i.getQuantiteDeclaree());
                    });
        }
        sacInfecteItemRepository.saveAll(items);

        int ecartGlobal = totalCompte - sac.getQuantiteDeclaree();
        sac.setQuantiteCompteePostLavage(totalCompte);
        sac.setEcart(ecartGlobal);
        sac.setDateVerification(LocalDateTime.now());
        sac.setAgentLavage(agent);
        sac.setObservations((sac.getObservations() != null ? sac.getObservations() + " | " : "")
                + (request.getObservations() != null ? request.getObservations() : ""));
        sac.setEtat(ecartGlobal == 0 ? EtatSac.VERIFIE : EtatSac.ECART_DETECTE);
        sacInfecteRepository.save(sac);
        if (sac.getCycleLavage() != null) {
            CycleLavage cycle = sac.getCycleLavage();
            cycle.setStatut(StatutCycle.TERMINE);
            cycle.setDateFin(LocalDateTime.now());
            cycle.getMachine().setEtat(Machine.EtatMachine.DISPONIBLE);
            machineRepository.save(cycle.getMachine());
            cycleLavageRepository.save(cycle);
        }
        Collecte collecte = sac.getCollecte();

        if (collecte != null) {
            if (sac.getEtat() == EtatSac.VERIFIE) {
                collecte.setEtatCollecte(EtatCollecte.verifie);
            } else {
                collecte.setEtatCollecte(EtatCollecte.non_valide);
            }

            collecteRepository.save(collecte);
        }
        return toDTO(sac);
    }

    //  SCAN LINGES PROPRES → RÉINTÉGRATION FLUX NORMAL
    public SacInfecteDTO affecterLingesPropres(AffectationLingesPropresRequest request) {
        SacInfecte sac = sacInfecteRepository.findByNumeroSac(request.getNumeroSac())
                .orElseThrow(() -> new ResourceNotFoundException("SacInfecte", "numeroSac", request.getNumeroSac()));

        if (sac.getEtat() != EtatSac.VERIFIE && sac.getEtat() != EtatSac.ECART_DETECTE)
            throw new BusinessException("Le sac doit être VERIFIE ou ECART_DETECTE. État actuel : " + sac.getEtat());

        if (request.getCodesLinges() == null || request.getCodesLinges().isEmpty())
            throw new BusinessException("Aucun code-barre fourni.");

        Agent agent = agentRepository.findByMatricule(request.getMatriculeAgent())
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "matricule", request.getMatriculeAgent()));

        Collecte collecte = sac.getCollecte();

        if (collecte != null) {
            if (sac.getEtat() == EtatSac.VERIFIE) {
                collecte.setEtatCollecte(EtatCollecte.verifie);
            } else {
                collecte.setEtatCollecte(EtatCollecte.non_valide);
            }

            collecteRepository.save(collecte);
        }

        for (String codeBarre : request.getCodesLinges()) {
            Linge linge = lingeRepository.findByCodeBarre(codeBarre)
                    .orElseThrow(() -> new ResourceNotFoundException("Linge", "codeBarre", codeBarre));

            EtatLinge etatAvant = linge.getEtat();
            // Réintégration dans le flux normal
            linge.setEtat(EtatLinge.LAVE);
            linge.setTypeCollecte(TypeCollecte.INFECTE);
            linge.setObservations(
                    "Linge infecté lavé"
                            + " | Sac: " + sac.getNumeroSac()
                            + " | Collecte #" + (collecte != null ? collecte.getId() : "N/A")
                            + " | Agent collecte: " + sac.getAgentCollecte().getMatricule()
                            + " | Service: " + sac.getServiceId()
            );
            lingeRepository.save(linge);

            //  Rattacher à la collecte parente comme les linges blancs
            // Garantit qu'en livraison on retrouve TOUS les linges (blanc + infecté)
            if (collecte != null) {
                collecteLingeRepository.save(
                        CollecteLinge.builder()
                                .collecte(collecte)
                                .linge(linge)
                                .typeCollecte(TypeCollecte.INFECTE)
                                .numeroSacInfecte(sac.getNumeroSac())
                                .build()
                );
            }

            // Historique avec vrais liens
            historiqueLingeRepository.save(HistoriqueLinge.builder()
                    .linge(linge)
                    .etatAvant(etatAvant)
                    .etatApres(EtatLinge.LAVE)
                    .agent(agent)
                    .typeOperation(TypeOperation.LAVAGE_SAC_INFECTE)
                    .dateOperation(LocalDateTime.now())
                    .collecte(collecte)
                    .sacInfecte(sac)
                    .observations(
                            "Scan post-lavage infecté"
                                    + " | Sac: " + sac.getNumeroSac()
                                    + " | Collecte #" + (collecte != null ? collecte.getId() : "N/A")
                                    + " | Agent collecte: " + sac.getAgentCollecte().getMatricule()
                    )
                    .build());
            sac.getLingesPropres().add(linge);
        }

        sac.setEtat(EtatSac.LINGES_AFFECTES);
        sac.setDateAffectationPropres(LocalDateTime.now());
        sac.setAgentAffectation(agent);
        sacInfecteRepository.save(sac);
        return toDTO(sac);
    }

    //  CONSULTATIONS

    public List<SacInfecteDTO> getTousSacs() {
        return sacInfecteRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<SacInfecteDTO> getSacsParCollecte(Long collecteId) {
        return sacInfecteRepository.findByCollecte_Id(collecteId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<SacInfecteDTO> getSacsEnAttenteVerification() {
        return sacInfecteRepository.findSacsEnAttenteVerification().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<SacInfecteDTO> getSacsNonLaves() {
        return sacInfecteRepository.findSacsNonLaves().stream().map(this::toDTO).collect(Collectors.toList());
    }
    public List<SacInfecteDTO> getSacsAvecEcart() {
        return sacInfecteRepository.findSacsAvecEcart().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<SacInfecteDTO> getSacsParAgent(String matricule) {
        return sacInfecteRepository.findByAgentMatricule(matricule).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SacInfecteDTO getSacByNumero(String numeroSac) {
        return toDTO(sacInfecteRepository.findByNumeroSac(numeroSac)
                .orElseThrow(() -> new ResourceNotFoundException("SacInfecte", "numeroSac", numeroSac)));
    }
    public long countByCollecteId(Long collecteId) {
        return sacInfecteRepository.countByCollecte_Id(collecteId);
    }

    // MAPPING

    private SacInfecteDTO toDTO(SacInfecte sac) {
        List<SacInfecteDTO.ItemDTO> itemDTOs = sac.getItems().stream()
                .map(i -> SacInfecteDTO.ItemDTO.builder()
                        .categorie(i.getCategorie())
                        .quantiteDeclaree(i.getQuantiteDeclaree())
                        .quantiteComptee(i.getQuantiteComptee())
                        .ecart(i.getEcart())
                        .build())
                .collect(Collectors.toList());

        List<String> codesLingesPropres = sac.getLingesPropres().stream()
                .map(Linge::getCodeBarre).collect(Collectors.toList());

        return SacInfecteDTO.builder()
                .id(sac.getId())
                .numeroSac(sac.getNumeroSac())
                .etat(sac.getEtat())
                .collecteId(sac.getCollecte() != null ? sac.getCollecte().getId() : null)
                .agentCollecteNom(sac.getAgentCollecte() != null ? sac.getAgentCollecte().getNom() : null)
                .agentCollecteMatricule(sac.getAgentCollecte() != null ? sac.getAgentCollecte().getMatricule() : null)
               // .serviceNom(sac.getService() != null ? sac.getService().getNom() : null)
                .chambre(sac.getChambre())
                .quantiteDeclaree(sac.getQuantiteDeclaree())
                .quantiteCompteePostLavage(sac.getQuantiteCompteePostLavage())
                .ecart(sac.getEcart())
                .concordance(sac.getEcart() != null && sac.getEcart() == 0)
                .dateCollecte(sac.getDateCollecte())
                .dateReception(sac.getDateReception())
                .dateLavage(sac.getDateLavage())
                .dateVerification(sac.getDateVerification())
                .agentLavageNom(sac.getAgentLavage() != null ? sac.getAgentLavage().getNom() : null)
                .observations(sac.getObservations())
                .items(itemDTOs)
                .codesLingesPropres(codesLingesPropres)
                .nombreLingesAffectes(codesLingesPropres.size())
                .cycleLavageId(sac.getCycleLavage() != null ? sac.getCycleLavage().getId() : null)
                .machineCode(sac.getCycleLavage() != null && sac.getCycleLavage().getMachine() != null
                        ? sac.getCycleLavage().getMachine().getCode() : null)
                .build();
    }
}
