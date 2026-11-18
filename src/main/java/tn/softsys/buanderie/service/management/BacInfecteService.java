package tn.softsys.buanderie.service.management;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.BacInfecteDTO;
import tn.softsys.buanderie.dto.BacInfecteItemRequest;
import tn.softsys.buanderie.dto.BacInfecteRequest;
import tn.softsys.buanderie.entity.Bac;
import tn.softsys.buanderie.entity.BacQuantite;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.BacQuantiteRepository;
import tn.softsys.buanderie.repository.BacRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Service
@RequiredArgsConstructor
public class BacInfecteService {

    private final BacRepository bacRepository;
    private final BacQuantiteRepository bacQuantiteRepository;
    private final PersonnelRepository personnelRepository;
    private final ServiceRepository serviceRepository;
    private final ProfilExploitationService profilExploitationService;

    @Transactional
    public BacInfecteDTO declarerQuantites(BacInfecteRequest request, Collecte collecte) {
        profilExploitationService.exigerCircuitInfecte();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Impossible de déclarer un bac sans quantité.");
        }

        Personnel agent = request.getMatriculeAgent() == null
                ? null
                : personnelRepository.findByCodeBarre(request.getMatriculeAgent());
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", request.getMatriculeAgent());
        }

        if (request.getBacId() == null) {
            throw new BusinessException("Scannez le bac infecté qui reçoit ces quantités.");
        }
        Bac bac = bacRepository.findById(request.getBacId())
                .orElseThrow(() -> new BusinessException("Bac introuvable id=" + request.getBacId()));
        if (bac.getTypebac() != TypeBac.INFECTE) {
            throw new BusinessException("Le bac [" + bac.getCode() + "] n'est pas un bac infecté (ROUGE).");
        }

        Long serviceId = request.getServiceId();
        if (serviceId == null && collecte != null) {
            serviceId = collecte.getService();
        }
        if (serviceId == null) {
            throw new BusinessException("Le service est obligatoire.");
        }
        final Long idService = serviceId;
        serviceRepository.findById(idService)
                .orElseThrow(() -> new BusinessException("Service introuvable : " + idService));

        int total = 0;
        List<BacQuantite> lignes = new ArrayList<>();
        for (BacInfecteItemRequest item : request.getItems()) {
            if (item.getCategorie() == null) {
                throw new BusinessException("La catégorie est obligatoire.");
            }
            if (item.getQuantiteDeclaree() == null || item.getQuantiteDeclaree() <= 0) {
                throw new BusinessException("Quantité invalide pour : " + item.getCategorie());
            }
            total += item.getQuantiteDeclaree();
            lignes.add(BacQuantite.builder()
                    .bac(bac)
                    .categorie(item.getCategorie())
                    .quantiteDeclaree(item.getQuantiteDeclaree())
                    .collecteId(collecte != null ? collecte.getId() : null)
                    .serviceId(idService)
                    .personnelId(agent.getId())
                    .chambre(request.getChambre())
                    .observations(request.getObservations())
                    .dateDeclaration(LocalDateTime.now())
                    .build());
        }
        bacQuantiteRepository.saveAll(lignes);

        if (bac.getEtat() == null || bac.getEtat() == EtatBac.EN_ATTENTE_COLLECTE || bac.getEtat() == EtatBac.VIDE) {
            bac.setEtat(EtatBac.COLLECTE);
            bacRepository.save(bac);
        }

        List<BacInfecteDTO.Item> items = new ArrayList<>();
        for (BacQuantite ligne : lignes) {
            items.add(new BacInfecteDTO.Item(ligne.getCategorie().name(), ligne.getQuantiteDeclaree()));
        }

        return BacInfecteDTO.builder()
                .bacId(bac.getId())
                .codeBac(bac.getCode())
                .collecteId(collecte != null ? collecte.getId() : null)
                .serviceId(idService)
                .quantiteTotale(total)
                .items(items)
                .build();
    }
}
