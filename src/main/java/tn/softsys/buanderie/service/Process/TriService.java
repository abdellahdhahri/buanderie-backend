package tn.softsys.buanderie.service.Process;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
import tn.softsys.buanderie.dto.Lot.AffectationLotRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TriService {

    private final LingeRepository lingeRepository;
    private final BacRepository bacRepository;
    private final AgentRepository agentRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final LotRepository lotRepository;
    private final SqlServerPersonnelRepository sqlPersonnelRepo;


    @Transactional
    public Lot affecterAuLot(AffectationLotRequest request) {
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(request.getMatriculeAgent());
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Agent", "matricule", request.getMatriculeAgent()));
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
        if (linge.getEtat() != EtatLinge.RECEPTION) {
            throw new BusinessException("Le linge doit être en RECEPTION.");
        }
        Lot lot = lotRepository.findById(request.getLotId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lot", "id", request.getLotId()));

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

        if (lot.getPoidsActuel() + poidsLinge > lot.getCapaciteMax()) {
            throw new BusinessException(
                    "Lot plein.");
        }

        // Affectation
        linge.setLot(lot);
        linge.setEtat(EtatLinge.EN_TRI);
        linge.setBac(null);

        lingeRepository.save(linge);
        // Mise à jour poids
        lot.setPoidsActuel(lot.getPoidsActuel() + poidsLinge);

        if (lot.getPoidsActuel() >= lot.getCapaciteMax()) {
            lot.setEtat(EtatLot.PRET_LAVAGE);
        }
        lotRepository.save(lot);
        historiqueLingeRepository.save(
                HistoriqueLinge.builder()
                        .linge(linge)
                        .etatAvant(EtatLinge.RECEPTION)
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

        if (linge.getEtat() != EtatLinge.RECEPTION) {
            throw new BusinessException(
                    "Le linge doit être en état RECEPTION.");
        }

        double poidsLinge = linge.getPoids() == null ? 0.0 : linge.getPoids();

        Linge finalLinge = linge;
        //Linge finalLinge1 = linge;
        return lotRepository.findAll().stream()
                // meme catégorie
                .filter(l -> l.getCategorie() == finalLinge.getCategorie())
                // meme type de collecte
                .filter(l -> l.getTypeCollecte() == finalLinge.getTypeCollecte())
                // uniquement les lots en cours de tri
                .filter(l -> l.getEtat() == EtatLot.EN_TRI)
                // capacité suffisante
                .filter(l -> l.getPoidsActuel() + poidsLinge <= l.getCapaciteMax())
                // tri du moins rempli au plus rempli
                .sorted(Comparator.comparing(Lot::getPoidsActuel))
                .toList();
    }
    private Linge trouverLinge(String codeOuTag) {
        return lingeRepository.findByCodeBarre(codeOuTag)
                .orElseGet(() -> lingeRepository.findByRfidTag(codeOuTag));

    }

}