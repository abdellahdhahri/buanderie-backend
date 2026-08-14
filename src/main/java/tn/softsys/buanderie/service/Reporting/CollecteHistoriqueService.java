package tn.softsys.buanderie.service.Reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.dto.CollecteHistoriqueDTO;
import tn.softsys.buanderie.dto.CollecteListeDTO;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.CollecteLinge;
import tn.softsys.buanderie.entity.SacInfecte;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.CollecteLingeRepository;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.repository.SacInfecteRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollecteHistoriqueService {

    private final CollecteRepository          collecteRepo;
    private final CollecteLingeRepository     collecteLingeRepo;
    private final SacInfecteRepository        sacInfecteRepo;
    private final SqlServerServiceRepository  sqlServiceRepo;

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

    public List<CollecteListeDTO> getCollectesParAgent(String matricule) {
        return collecteRepo.findByAgent_Matricule(matricule).stream()
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
        List<SacInfecte> sacs = sacInfecteRepo.findByCollecte_Id(collecteId);

        String serviceCode = null;
        if (c.getService() != null) {
            serviceCode = sqlServiceRepo.findById(c.getService())
                    .map(s -> s.getCode()).orElse(null);
        }

        int total = lignes.size() + sacs.stream()
                .mapToInt(s -> s.getQuantiteDeclaree() != null ? s.getQuantiteDeclaree() : 0)
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

        List<CollecteHistoriqueDTO.SacInfecteResumeDTO> sacsDTO = sacs.stream()
                .map(sac -> CollecteHistoriqueDTO.SacInfecteResumeDTO.builder()
                        .id(sac.getId())
                        .numeroSac(sac.getNumeroSac())
                        .etat(sac.getEtat() != null ? sac.getEtat().name() : null)
                        .quantiteDeclaree(sac.getQuantiteDeclaree())
                        .quantiteCompteePostLavage(sac.getQuantiteCompteePostLavage())
                        .ecart(sac.getEcart())
                        .dateCollecte(sac.getDateCollecte())
                        .dateReception(sac.getDateReception())
                        .items(sac.getItems() != null
                                ? sac.getItems().stream().map(item ->
                                CollecteHistoriqueDTO.SacInfecteResumeDTO.SacItemDTO.builder()
                                .categorie(item.getCategorie() != null
                                           ? item.getCategorie().name() : null)
                                .quantiteDeclaree(item.getQuantiteDeclaree())
                                .quantiteComptee(item.getQuantiteComptee())
                                .ecart(item.getEcart())
                                .build())
                                  .collect(Collectors.toList())
                                : List.of())
                        .build())
                .collect(Collectors.toList());

        return CollecteHistoriqueDTO.builder()
                .id(c.getId())
                .numeroCollecte(c.getNumeroCollecte())
                .typeCollecte(c.getTypeCollecte() != null ? c.getTypeCollecte().name() : null)
                .etatCollecte(c.getEtatCollecte() != null ? c.getEtatCollecte().name() : null)
                .agentMatricule(c.getAgent() != null ? c.getAgent().getMatricule() : null)
                .agentNom(c.getAgent() != null
                        ? c.getAgent().getNom() + " " + c.getAgent().getPrenom() : null)
                .serviceId(c.getService())
                .serviceCode(serviceCode)
                .chambreIdSqlServer(c.getChambreIdSqlServer())
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
                .sacsInfectes(sacsDTO)
                .build();
    }

    private CollecteListeDTO toListeDTO(Collecte c) {
        String serviceCode = null;
        if (c.getService() != null) {
            serviceCode = sqlServiceRepo.findById(c.getService())
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
                .agentMatricule(c.getAgent() != null ? c.getAgent().getMatricule() : null)
                .agentNom(c.getAgent() != null
                        ? c.getAgent().getNom() + " " + c.getAgent().getPrenom() : null)
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
