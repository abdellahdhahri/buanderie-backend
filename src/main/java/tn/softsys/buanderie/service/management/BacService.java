package tn.softsys.buanderie.service.management;

import tn.softsys.buanderie.entity.enums.TypeCollecte;

import tn.softsys.buanderie.entity.enums.TypeBac;

import tn.softsys.buanderie.entity.enums.CategLinge;

import tn.softsys.buanderie.entity.enums.UsageBac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.softsys.buanderie.dto.BacDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;

@Service
@RequiredArgsConstructor
@Transactional
public class BacService {

    private final BacRepository bacRepository;
    private final AgentRepository agentRepository;
    private final HistoriqueBacRepository historiqueBacRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final ServiceRepository serviceRepository;


    public List<Bac> findAll() {
        return bacRepository.findAll();
             /*   .stream()
                .map(BacDTO::from)
                .toList();*/
    }

    public Bac findById(Long id) {
        return bacRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bac", id));
    }
    public Bac findByRfidTag(String code){
        return bacRepository.findByRfidTag(code)
                .orElseThrow(() -> new ResourceNotFoundException("Bac introuvable"));

    }

    public Bac findByReference(String reference) {
        return bacRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Bac", "reference", reference));
    }

    public List<Bac> findByEtat(EtatBac etat) {
        return bacRepository.findByEtat(etat);
    }

    public List<BacDTO> listerBacsDeTri(EtatBac etat, CategLinge categorie, TypeCollecte typeCollecte) {
        return bacRepository.findByUsage(UsageBac.TRI).stream()
                .filter(b -> etat == null || b.getEtat() == etat)
                .filter(b -> categorie == null || b.getCategorie() == categorie)
                .filter(b -> typeCollecte == null || b.getTypeCollecte() == typeCollecte)
                .map(BacDTO::from)
                .toList();
    }

    /**
     * Modifie les champs internes d'un bac : seuls les champs fournis changent. Pour un bac importé de CUBIX,
     * le code et la désignation appartiennent à CUBIX (ils ne sont pas modifiés ici) ; l'état du bac reste géré
     * par les opérations (tri, lavage...).
     */
    @Transactional
    public Bac modifier(Long id, BacDTO dto) {
        Bac bac = bacRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bac", id));
        boolean externe = bac.getSourceDonnee() == tn.softsys.buanderie.entity.enums.SourceDonnee.EXTERNE;

        if (!externe) {
            if (dto.getCode() != null && !dto.getCode().isBlank() && !dto.getCode().equals(bac.getCode())) {
                if (bacRepository.findByCode(dto.getCode()).isPresent()) {
                    throw new BusinessException("Le code « " + dto.getCode() + " » est déjà utilisé par un autre bac.");
                }
                bac.setCode(dto.getCode().trim());
            }
            if (dto.getDesignation() != null) {
                bac.setDesignation(dto.getDesignation().isBlank() ? null : dto.getDesignation().trim());
            }
        }
        if (dto.getTypebac() != null) {
            bac.setTypebac(dto.getTypebac());
        }
        if (dto.getUsage() != null) {
            bac.setUsage(dto.getUsage());
        }
        if (dto.getCategorie() != null) {
            bac.setCategorie(dto.getCategorie());
        }
        if (dto.getTypeCollecte() != null) {
            bac.setTypeCollecte(dto.getTypeCollecte());
        }
        if (dto.getCapacite_max() != null) {
            bac.setCapacite_max(dto.getCapacite_max());
        }
        if (dto.getObservations() != null) {
            bac.setObservations(dto.getObservations());
        }
        return bacRepository.save(bac);
    }

    public Bac creer(BacDTO bacDTO) {
        if (bacRepository.existsByReference(bacDTO.getReference())) {
            throw new BusinessException("Un bac avec la référence '" + bacDTO.getReference() + "' existe déjà");
        }

//        ServiceEntity service = null;
//        if (bacDTO.getService() != null) {
//            service = serviceRepository.findById(bacDTO.getService().getId()).orElse(null);
//        }
        UsageBac usage = bacDTO.getUsage() != null ? bacDTO.getUsage() : UsageBac.COLLECTE;
        boolean bacDeTri = usage == UsageBac.TRI;

        Bac bac = Bac.builder()
                .reference(bacDTO.getReference())
                .etat(bacDeTri ? EtatBac.VIDE : EtatBac.EN_ATTENTE_COLLECTE)
                .usage(usage)
                .categorie(bacDTO.getCategorie())
                .poidsBalance(bacDTO.getPoidsBalance())
                .observations(bacDTO.getObservations())
                .poids(bacDTO.getPoids())
                .typeCollecte(bacDTO.getTypeCollecte() != null
                        ? bacDTO.getTypeCollecte()
                        : (bacDeTri ? TypeCollecte.BLANC : null))
                .code(bacDTO.getCode())
                .capacite_max((bacDTO.getCapacite_max()))
                //.service(service)

                .typebac(bacDTO.getTypebac() != null
                        ? bacDTO.getTypebac()
                        : (bacDeTri ? TypeBac.BLANC : null))
                .build();
        return bacRepository.save(bac);
    }

//    private void changerEtatLingesDuBac(
//            Bac bac,
//            EtatLinge nouvelEtat,
//            Agent agent,
//            String observations) {
//
//        for (Linge linge : bac.getLinges()) {
//
//            EtatLinge ancienEtat = linge.getEtat();
//
//            linge.setEtat(nouvelEtat);
//
//            lingeRepository.save(linge);
//
//            HistoriqueLinge historique = HistoriqueLinge.builder()
//                    .linge(linge)
//                    .etatAvant(ancienEtat)
//                    .etatApres(nouvelEtat)
//                    .agent(agent)
//                    .typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
//                    .dateOperation(LocalDateTime.now())
//                    .observations(observations)
//                    .build();
//
//            historiqueLingeRepository.save(historique);
//        }
//    }
//    public List<Bac> getBacsByService(Long serviceId) {
//        return bacRepository.findByService_Id(serviceId);
//    }


    public List<HistoriqueBac> getHistoriqueBac(Long bacId) {
        return historiqueBacRepository.findByBacIdOrderByDateOperationDesc(bacId);
    }

    public Optional<Bac> getByCodeBac(String code){
        return bacRepository.findByCode(code);
    }


    private void changerEtatBac(Bac bac, EtatBac nouvelEtat, Agent agent,
                                TypeOperation operation, String observations) {
        EtatBac ancienEtat = bac.getEtat();
        bac.setEtat(nouvelEtat);
        bacRepository.save(bac);

        HistoriqueBac historique = HistoriqueBac.builder()
                .bac(bac)
                .etatAvant(ancienEtat)
                .etatApres(nouvelEtat)
                .agent(agent)
                .typeOperation(operation)
                .dateOperation(LocalDateTime.now())
                .observations(observations)
                .build();
        historiqueBacRepository.save(historique);
    }

    private Agent resolveAgent(String matricule) {
        return agentRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "matricule", matricule));
    }
    public Bac viderBac(Long id) {
        Bac bac = bacRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bac", id));

        if (bac.getLinges() != null) {
            for (Linge linge : bac.getLinges()) {
                linge.setBac(null);
            }
            bac.getLinges().clear();
        }

        bac.setPoids(0.0);
        bac.setPoidsBalance(null);
        if (bac.getUsage() == UsageBac.TRI) {
            bac.setEtat(EtatBac.VIDE);
        }

        return bacRepository.save(bac);
    }
    public void delete(Long id) {
        Bac bac = bacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bac introuvable avec l'id : " + id));

        bacRepository.delete(bac);
    }
}
