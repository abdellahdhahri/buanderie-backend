package tn.softsys.buanderie.service.Reporting;

import java.util.Map;

import java.util.LinkedHashMap;

import tn.softsys.buanderie.dto.Collectes.CollecteHistoriqueDTO;
import tn.softsys.buanderie.dto.Collectes.CollecteListeDTO;
import tn.softsys.buanderie.entity.BacQuantite;

import tn.softsys.buanderie.entity.Bac;

import tn.softsys.buanderie.repository.BacQuantiteRepository;
import tn.softsys.buanderie.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.CollecteLinge;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.CollecteLingeRepository;
import tn.softsys.buanderie.repository.CollecteRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollecteHistoriqueService {

    private final CollecteRepository          collecteRepo;
    private final CollecteLingeRepository     collecteLingeRepo;
    private final BacQuantiteRepository bacQuantiteRepo;
    private final ServiceRepository  serviceRepository;

    public List<CollecteListeDTO> getAllCollectes() {
        return collecteRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getDateCollecte() == null) return 1;
                    if (b.getDateCollecte() == null) return -1;
                    return b.getDateCollecte().compareTo(a.getDateCollecte());
                })
                .map(this::toListeDTO)
                .collect(Collectors.toList());
    }

    public List<CollecteListeDTO> getCollectesParAgent(Long id) {
        return collecteRepo.findByPersonnelId(id).stream()
                .sorted((a, b) -> {
                    if (a.getDateCollecte() == null) return 1;
                    if (b.getDateCollecte() == null) return -1;
                    return b.getDateCollecte().compareTo(a.getDateCollecte());
                })
                .map(this::toListeDTO)
                .collect(Collectors.toList());
    }

    public List<CollecteListeDTO> getCollectesParService(Long serviceId) {
        return collecteRepo.findByService(serviceId).stream()
                .map(this::toListeDTO)
                .collect(Collectors.toList());
    }

    public CollecteHistoriqueDTO getDetailCollecte(Long collecteId) {
        Collecte c = collecteRepo.findById(collecteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collecte", "id", collecteId));

        List<CollecteLinge> lignes = collecteLingeRepo.findByCollecte_Id(collecteId);
        List<BacQuantite> quantites = bacQuantiteRepo.findByCollecteId(collecteId);

        String serviceCode = null;
        if (c.getService() != null) {
            serviceCode = serviceRepository.findById(c.getService())
                    .map(s -> s.getCode()).orElse(null);
        }

        int total = lignes.size() + quantites.stream()
                .mapToInt(q -> q.getQuantiteDeclaree() != null ? q.getQuantiteDeclaree() : 0)
                .sum();

        List<CollecteHistoriqueDTO.CollecteLingeDTO> lingesDTO = lignes.stream()
                .map(cl -> CollecteHistoriqueDTO.CollecteLingeDTO.builder()
                        .lingeId(cl.getLinge() != null ? cl.getLinge().getId() : null)
                        .codeBarre(cl.getLinge() != null ? cl.getLinge().getCodeBarre() : null)
                        .rfidTag(cl.getLinge() != null ? cl.getLinge().getRfidTag() : null)
                        .categorie(cl.getLinge() != null && cl.getLinge().getCategorie() != null
                                ? cl.getLinge().getCategorie().name() : null)
                        .etatActuel(cl.getLinge() != null && cl.getLinge().getEtat() != null
                                ? cl.getLinge().getEtat().name() : null)
                        .typeCollecte(cl.getTypeCollecte() != null
                                ? cl.getTypeCollecte().name() : null)
                        .statutCollecte(cl.getLinge() != null
                                && cl.getLinge().getStatutCollecte() != null
                                ? cl.getLinge().getStatutCollecte().name() : null)
                        .dateScan(cl.getDateScan())
                        .codeBac(cl.getLinge() != null && cl.getLinge().getBac() != null
                                ? cl.getLinge().getBac().getCode() : null)
                        .numeroSacInfecte(cl.getNumeroSacInfecte())
                        .build())
                .collect(Collectors.toList());

        Map<Long, List<BacQuantite>> parBac = quantites.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getBac().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<CollecteHistoriqueDTO.BacInfecteResumeDTO> bacsDTO = parBac.values().stream()
                .map(lignesBac -> {
                    Bac bac = lignesBac.get(0).getBac();
                    int totalBac = lignesBac.stream()
                            .mapToInt(q -> q.getQuantiteDeclaree() != null ? q.getQuantiteDeclaree() : 0)
                            .sum();
                    return CollecteHistoriqueDTO.BacInfecteResumeDTO.builder()
                            .bacId(bac.getId())
                            .codeBac(bac.getCode())
                            .quantiteDeclaree(totalBac)
                            .dateDeclaration(lignesBac.get(0).getDateDeclaration())
                            .items(lignesBac.stream()
                                    .map(q -> CollecteHistoriqueDTO.BacInfecteResumeDTO.ItemDTO.builder()
                                            .categorie(q.getCategorie() != null ? q.getCategorie().name() : null)
                                            .quantiteDeclaree(q.getQuantiteDeclaree())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());

        return CollecteHistoriqueDTO.builder()
                .id(c.getId())
                .numeroCollecte(c.getNumeroCollecte())
                .typeCollecte(c.getTypeCollecte() != null ? c.getTypeCollecte().name() : null)
                .etatCollecte(c.getEtatCollecte() != null ? c.getEtatCollecte().name() : null)
                //    .agentMatricule(c.getAgent() != null ? c.getAgent().getMatricule() : null)
                //   .agentNom(c.getAgent() != null
                //   ? c.getAgent().getNom() + " " + c.getAgent().getPrenom() : null)
                .serviceId(c.getService())
                .serviceCode(serviceCode)
                .chambreId(c.getChambreId())
                .codeChambre(c.getCodeChambre())
                .dateCollecte(c.getDateCollecte())
                .dateFin(c.getDateFin())
                .nbAttendus(c.getNbAttendus())
                .nbBlancs(c.getNbScannésBlanc())
                .nbInfectes(c.getNbInfectésPrésumés())
                .nbPropres(c.getNbPropres())
                .nbManquants(c.getNbManquants())
                .nbTotal(total)
                .linges(lingesDTO)
                .bacsInfectes(bacsDTO)
                .build();
    }

    private CollecteListeDTO toListeDTO(Collecte c) {
        String serviceCode = null;
        if (c.getService() != null) {
            serviceCode = serviceRepository.findById(c.getService())
                    .map(s -> s.getCode()).orElse(null);
        }
        int nbTotal = (c.getNbScannésBlanc() != null ? c.getNbScannésBlanc() : 0)
                + (c.getNbInfectésPrésumés() != null ? c.getNbInfectésPrésumés() : 0)
                + (c.getNbPropres() != null ? c.getNbPropres() : 0)
                + (c.getNbManquants() != null ? c.getNbManquants() : 0);

        return CollecteListeDTO.builder()
                .id(c.getId())
                .numeroCollecte(c.getNumeroCollecte())
                .typeCollecte(c.getTypeCollecte() != null ? c.getTypeCollecte().name() : null)
                .etatCollecte(c.getEtatCollecte() != null ? c.getEtatCollecte().name() : null)
                // .agentMatricule(c.getAgent() != null ? c.getAgent().getMatricule() : null)
                //.agentNom(c.getAgent() != null
                //      ? c.getAgent().getNom() + " " + c.getAgent().getPrenom() : null)
                .serviceId(c.getService())
                .serviceCode(serviceCode)
                .codeChambre(c.getCodeChambre())
                .dateCollecte(c.getDateCollecte())
                .dateFin(c.getDateFin())
                .nbAttendus(c.getNbAttendus())
                .nbBlancs(c.getNbScannésBlanc())
                .nbInfectes(c.getNbInfectésPrésumés())
                .nbPropres(c.getNbPropres())
                .nbManquants(c.getNbManquants())
                .nbTotal(nbTotal)
                .build();
    }
}