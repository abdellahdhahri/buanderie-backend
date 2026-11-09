package tn.softsys.buanderie.service.Process;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.dto.Lot.AffectationLotRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.management.ParcoursService;

@Service
@RequiredArgsConstructor
@Transactional
public class TriService {

    private final LingeRepository lingeRepository;
    private final BacRepository bacRepository;
    private final AgentRepository agentRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final LotRepository lotRepository;
    private final PersonnelRepository sqlPersonnelRepo;
    private final ParcoursService parcoursService;


    @Transactional
    public Lot affecterAuLot(AffectationLotRequest request) {
        parcoursService.exigerActive(CodeEtape.TRI);

        Personnel agent = sqlPersonnelRepo.findByCodeBarre(request.getMatriculeAgent());
        if (agent == null) {
            throw new BusinessException("Agent introuvable : " + request.getMatriculeAgent());
        }
        Linge linge = lingeRepository.findByCodeBarre(request.getCodeOuTag())
                .orElse(null);
        if (linge == null) {
            linge = lingeRepository.findByRfidTag(request.getCodeOuTag());
        }
        if (linge == null) {
            throw new ResourceNotFoundException(
                    "Linge",
                    "codeBarre/rfidTag",
                    request.getCodeOuTag());
        }
        Set<EtatLinge> etatsAcceptes = etatsAcceptesTri();
        if (!etatsAcceptes.contains(linge.getEtat())) {
            throw new BusinessException("Le linge doit être dans l'un des états suivants : "
                    + String.join(", ", etatsAcceptes.stream().map(Enum::name).toList())
                    + " (état actuel : " + linge.getEtat() + ").");
        }
        Lot lot = lotRepository.findById(request.getLotId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lot", "id", request.getLotId()));

        if (lot.getEtat() != EtatLot.EN_TRI) {
            throw new BusinessException("Le lot « " + lot.getCodeLot()
                    + " » n'est pas ouvert (état : " + lot.getEtat() + ").");
        }

        // Vérification catégorie
        if (!lot.getCategorie().equals(linge.getCategorie())) {
            throw new BusinessException(
                    "Le lot n'est pas destiné à cette catégorie.");
        }

        // Vérification type
        if (lot.getTypeCollecte() != linge.getTypeCollecte()) {
            throw new BusinessException(
                    "Le type de collecte ne correspond pas.");
        }

        double poidsLinge = linge.getPoids() == null ? 0 : linge.getPoids();

        EtatLinge etatAvant = linge.getEtat();

        // Affectation
        linge.setLot(lot);
        linge.setEtat(EtatLinge.EN_TRI);
        linge.setBac(null);

        lingeRepository.save(linge);
        // Mise à jour poids
        lot.setPoidsActuel(lot.getPoidsActuel() + poidsLinge);

        lotRepository.save(lot);
        historiqueLingeRepository.save(
                HistoriqueLinge.builder()
                        .linge(linge)
                        .etatAvant(etatAvant)
                        .etatApres(EtatLinge.EN_TRI)
                        .personnelId(agent.getId())
                        .typeOperation(TypeOperation.TRI)
                        .dateOperation(LocalDateTime.now())
                        .observations("Affectation au lot " + lot.getCodeLot())
                        .build());
        return lot;
    }

    //methode pour suggerer les  lots de tri pour une linge
    @Transactional(readOnly = true)
    public List<Lot> suggererLots(String codeOuTag) {
        // Recherche du linge (code-barres ou RFID)
        Linge linge = lingeRepository.findByCodeBarre(codeOuTag)
                .orElse(null);
        if (linge == null) {
            linge = lingeRepository.findByRfidTag(codeOuTag);
        }
        if (linge == null) {
            throw new ResourceNotFoundException(
                    "Linge",
                    "codeBarre/rfidTag",
                    codeOuTag);

        }

        if (!etatsAcceptesTri().contains(linge.getEtat())) {
            throw new BusinessException(
                    "Le linge n'est pas prêt pour le tri (état actuel : " + linge.getEtat() + ").");
        }

        Linge finalLinge = linge;
        //Linge finalLinge1 = linge;
        return lotRepository.findAll().stream()
                // meme catégorie
                .filter(l -> l.getCategorie() == finalLinge.getCategorie())
                // meme type de collecte
                .filter(l -> l.getTypeCollecte() == finalLinge.getTypeCollecte())
                // uniquement les lots en cours de tri
                .filter(l -> l.getEtat() == EtatLot.EN_TRI)
                // tri du moins rempli au plus rempli
                .sorted(Comparator.comparing(Lot::getPoidsActuel))
                .toList();
    }
    private Set<EtatLinge> etatsAcceptesTri() {
        Set<EtatLinge> etats = new LinkedHashSet<>(parcoursService.etatsAcceptes(CodeEtape.TRI));
        if (etats.isEmpty()) {
            etats.add(EtatLinge.RECEPTION);
        }
        return etats;
    }

    private Linge trouverLinge(String codeOuTag) {
        return lingeRepository.findByCodeBarre(codeOuTag)
                .orElseGet(() -> lingeRepository.findByRfidTag(codeOuTag));

    }

}