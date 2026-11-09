package tn.softsys.buanderie.service.management;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeCollecte;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.dto.Linge.AssocierRfidRequest;
import tn.softsys.buanderie.dto.Historique.HistoriqueLingeDto;
import tn.softsys.buanderie.dto.LingeDTO;
import tn.softsys.buanderie.entity.*;

import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import tn.softsys.buanderie.service.referentiel.CleReferentiel;

@Service
@RequiredArgsConstructor
@Transactional
public class LingeService {
    private final LingeRepository lingeRepository;
    private final BacRepository bacRepository;
    private final AgentRepository agentRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final CollecteRepository collecteRepository;
    private final ChambreRepository chambreRepository;
    private final ServiceRepository serviceRepository;
    private final LitRepository litRepository;
    private final  LotRepository lotRepository;
    private final PersonnelRepository personnelRepository;

    public Page<LingeDTO> getLingesPaged(
            int page,
            int size,
            String categorie,
            String etatStr,
            Long serviceId,
            String search
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "dateCreation")
        );

        Specification<Linge> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Filtre catégorie
            if (categorie != null && !categorie.trim().isEmpty()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("categorie")),
                                categorie.trim().toLowerCase()
                        )
                );
            }

            // Filtre état
            if (etatStr != null && !etatStr.trim().isEmpty()) {
                try {
                    EtatLinge etat =
                            EtatLinge.valueOf(etatStr.trim().toUpperCase());

                    predicates.add(
                            cb.equal(root.get("etat"), etat)
                    );

                } catch (IllegalArgumentException e) {
                    // État invalide
                }
            }

            // Filtre service ID
            if (serviceId != null) {
                predicates.add(
                        cb.equal(
                                root.get("serviceId"),
                                serviceId
                        )
                );
            }

            // Recherche
            if (search != null && !search.trim().isEmpty()) {

                String searchPattern =
                        "%" + search.trim().toLowerCase() + "%";

                Predicate codeBarrePred =
                        cb.like(
                                cb.lower(root.get("codeBarre")),
                                searchPattern
                        );

                Predicate designationPred =
                        cb.like(
                                cb.lower(root.get("designation")),
                                searchPattern
                        );

                predicates.add(
                        cb.or(
                                codeBarrePred,
                                designationPred
                        )
                );
            }

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };

        return lingeRepository
                .findAll(spec, pageable)
                .map(this::toDTO);
    }

    public List<Linge> findAll() {
        return lingeRepository.findAll();
    }

    public Linge findById(Long id) {
        return lingeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Linge", id));
    }

    public Linge findByCodeBarre(String code) {
        return lingeRepository.findByCodeBarre(code).orElseThrow(() -> new ResourceNotFoundException("Linge", "codeBarre", code));
    }

    public List<Linge> findByBac(Long bacId) {
        return lingeRepository.findByBacId(bacId);
    }

    @Transactional
    public LingeDTO createLinge(LingeDTO dto) {

        if (dto.getCodebarre() != null && !dto.getCodebarre().trim().isEmpty()) {
            if (lingeRepository.existsByCodeBarre(dto.getCodebarre().trim())) {
                throw new RuntimeException("Code barre déjà existant");
            }
        }

        ServiceEntity service = null;
        if (dto.getServiceId() != null) {
            service = serviceRepository.findById(dto.getServiceId()).orElse(null);
        }

        Bac bac = null;
        if (dto.getBacId() != null) {
            bac = bacRepository.findById(dto.getBacId()).orElse(null);
        }

        Chambre chambre = null;
        if (dto.getChambre_id() != null) {
            chambre = chambreRepository.findById(dto.getChambre_id()).orElse(null);
        }

        Lot lot = null;
        if (dto.getLotId() != null) {
            lot = lotRepository.findById(dto.getLotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lot", dto.getLotId()));
        }

        EtatLinge etat = dto.getEtat() != null ? dto.getEtat() : EtatLinge.STOCKE;

        Linge linge = Linge.builder()
                .categorie(CategLinge.valueOf(dto.getCategorie()))
                .chambreIdSqlServer(chambre != null ? CleReferentiel.cle(chambre) : null)
                .poids(dto.getPoids())
                .etat(etat)
                .serviceId(dto.getServiceId())
                .bac(bac)
                .lot(lot)
                .codeBarre(dto.getCodebarre())
                .build();


        Linge savedLinge = lingeRepository.save(linge);
        return toDTO(savedLinge);
    }

    // Dans LingeService.java
    @Transactional
    public Linge creer(LingeDTO dto) {
        if (lingeRepository.existsByCodeBarre(dto.getCodebarre())) {
            throw new BusinessException("Code barre " + dto.getCodebarre() + " existe déjà");
        }

        Linge linge = new Linge();
        linge.setCodeBarre(dto.getCodebarre());
        linge.setRfidTag(dto.getRfidTag());
        linge.setCategorie(CategLinge.valueOf(dto.getCategorie()));
        linge.setEtat(EtatLinge.STOCKE);
        linge.setTypeCollecte(TypeCollecte.BLANC);
        linge.setPoids(dto.getPoids());
        linge.setServiceId(dto.getServiceId());
        linge.setDureVie(dto.getDurevie());


        // Récupérer et assigner le service si serviceId est fourni
//        if (dto.getServiceId() != null) {
//            ServiceEntity service = serviceRepository.findById(dto.getServiceId())
//                    .orElseThrow(() -> new BusinessException("Service non trouvé avec l'ID: " + dto.getServiceId()));
//            linge.setServiceId(service);
//        }

        if (dto.getPorteurId() != null) {
            linge.setPorteurId(dto.getPorteurId());

            // Récupérer le nom du porteur depuis SQL Server
            try {
                Optional<Personnel> porteur = personnelRepository.findById(dto.getPorteurId());
                if (porteur != null) {
                    linge.setPorteurNom(porteur.get().getPrenom() + " " + porteur.get().getNom());
                }
            } catch (Exception e) {
                System.err.println("Erreur récupération porteur SQL Server: " + e.getMessage());
            }
        }



        // Définir la date de création
        linge.setDateCreation(LocalDateTime.now());

        return lingeRepository.save(linge);
    }

    public Linge changerEtat(Long lingeId, EtatLinge nouvelEtat, String matriculeAgent, String observations) {
        Linge linge = findById(lingeId);
        Personnel agent = personnelRepository.findByCodeBarre(matriculeAgent);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", matriculeAgent);
        }
        EtatLinge ancienEtat = linge.getEtat();
        linge.setEtat(nouvelEtat);
        lingeRepository.save(linge);
        HistoriqueLinge h = HistoriqueLinge.builder().linge(linge).etatAvant(ancienEtat).etatApres(nouvelEtat)
                .personnelId(agent.getId()).typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
                .dateOperation(LocalDateTime.now()).observations(observations).build();
        historiqueLingeRepository.save(h);
        return linge;
    }
    //Methode pour affecter une linge a un bac
    public Linge affecterAuBac(String codeBarre, Long bacId) {

        Linge linge = lingeRepository.findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                codeBarre));

        Bac bac = bacRepository.findById(bacId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bac",
                                bacId));

        linge.setBac(bac);

        return lingeRepository.save(linge);
    }
    public Page<HistoriqueLinge> getHistorique(Pageable pageable) { return historiqueLingeRepository.findAll(pageable); }


    private LingeDTO toDTO(Linge linge) {
        return LingeDTO.builder()
                .id(linge.getId())
                .categorie(
                        linge.getCategorie() != null
                                ? linge.getCategorie().name()
                                : null
                )
                .chambre_id(linge.getChambreIdSqlServer())
                .poids(linge.getPoids())
                .etat(linge.getEtat())
                .rfidTag(linge.getRfidTag())
                .codebarre(linge.getCodeBarre())

                .programmeLavage(linge.getProgrammeLavage())
                .programmeSechage(linge.getProgrammeSechage())

                .serviceId(linge.getServiceId())

                .bacId(
                        linge.getBac() != null
                                ? linge.getBac().getId()
                                : null
                )

                .porteurId(linge.getPorteurId())
                .porteurNom(linge.getPorteurNom())

                .durevie(linge.getDureVie())

                .build();
    }

    public List<HistoriqueLingeDto> getHistoriqueLingeByCodeBarre(String codeBarre) {

        return historiqueLingeRepository
                .findByLingeCodeBarreOrderByDateOperationDesc(codeBarre)
                .stream()
                .map(h -> HistoriqueLingeDto.builder()
                        .id(h.getId())
                        .lingeId(h.getLinge().getId())
                        .codeLinge(h.getLinge().getCodeBarre())
                        .agentId(h.getPersonnelId())
                        //.agentNom(h.getAgent().getNom())
                        .etatId(h.getLinge().getEtat())
                        .etatapres(h.getEtatApres())
                        .dateOperation(h.getDateOperation())
                        .observations(h.getObservations())
                        .build())
                .collect(Collectors.toList());
    }

    public List<Linge> getLingesByLotCode(String codeLot) {

        return lingeRepository.findByLot_CodeLot(codeLot);
    }
    public Linge getByRfidTag(String rfidTag){
        return lingeRepository.findByRfidTag(rfidTag);

    }


    //Methode pour associer  une linge existant a un tag RFID
    @Transactional
    public Linge associerTagRfid(AssocierRfidRequest request) {
        // Recherche du linge
        Linge linge = lingeRepository.findByCodeBarre(request.getCodeBarre())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                request.getCodeBarre()));

        if (lingeRepository.existsByRfidTag(request.getCodeRfid())) {

            throw new BusinessException(
                    "Le tag RFID est déjà associé à un autre linge.");
        }

        if (linge.getRfidTag() != null && !linge.getRfidTag().isBlank()) {

            throw new BusinessException(
                    "Ce linge possède déjà un tag RFID.");
        }

        // Association
        linge.setRfidTag(request.getCodeRfid());

        lingeRepository.save(linge);

        return linge;
    }


    @Transactional
    public Linge update(Long id, LingeDTO dto) {
        Linge linge = findById(id);

        if (dto.getCategorie() != null) {
            try {
                linge.setCategorie(CategLinge.valueOf(dto.getCategorie()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Catégorie invalide: " + dto.getCategorie());
            }
        }

        if (dto.getPoids() != null) {
            linge.setPoids(dto.getPoids());
        }

        if (dto.getEtat() != null) {
            linge.setEtat(dto.getEtat());
        }

        if (dto.getServiceId() != null) {
            linge.setServiceId(dto.getServiceId());
        }

        if (dto.getDurevie() != null) {
            linge.setDureVie(dto.getDurevie());
        }

        if (dto.getPorteurId() != null) {
            linge.setPorteurId(dto.getPorteurId());

            try {
                Personnel porteur = personnelRepository.findById(dto.getPorteurId()).orElse(null);
                if (porteur != null) {
                    linge.setPorteurNom(porteur.getPrenom() + " " + porteur.getNom());
                } else {
                    linge.setPorteurNom(null);
                }
            } catch (Exception e) {
                System.err.println("Erreur récupération porteur: " + e.getMessage());
                linge.setPorteurNom(null);
            }
        } else {
            linge.setPorteurId(null);
            linge.setPorteurNom(null);
        }

        if (dto.getRfidTag() != null) {
            linge.setRfidTag(dto.getRfidTag());
        }


        return lingeRepository.save(linge);
    }

}