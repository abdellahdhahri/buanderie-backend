package tn.softsys.buanderie.service.management;

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

    public Bac creer(BacDTO bacDTO) {
        if (bacRepository.existsByReference(bacDTO.getReference())) {
            throw new BusinessException("Un bac avec la référence '" + bacDTO.getReference() + "' existe déjà");
        }

//        ServiceEntity service = null;
//        if (bacDTO.getService() != null) {
//            service = serviceRepository.findById(bacDTO.getService().getId()).orElse(null);
//        }
        Bac bac = Bac.builder()
                .reference(bacDTO.getReference())
                .etat(EtatBac.EN_ATTENTE_COLLECTE)
                .observations(bacDTO.getObservations())
                .poids(bacDTO.getPoids())
                .typeCollecte(bacDTO.getTypeCollecte())
                .code(bacDTO.getCode())
                .capacite_max((bacDTO.getCapacite_max()))
                //.service(service)

                .typebac(bacDTO.getTypebac())
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

        return bacRepository.save(bac);
    }
    public void delete(Long id) {
        Bac bac = bacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bac introuvable avec l'id : " + id));

        bacRepository.delete(bac);
    }
}
