package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.Collectes.ResumeCollecteDTO;
import tn.softsys.buanderie.dto.ResumeCategorieDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.CollecteLingeRepository;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.repository.SacInfecteRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollecteResumeService {

    private final CollecteRepository collecteRepository;
    private final CollecteLingeRepository collecteLingeRepository;
    private final SacInfecteRepository sacInfecteRepository;

    public ResumeCollecteDTO getResume(Long collecteId) {

        Collecte collecte = collecteRepository.findById(collecteId)
                .orElseThrow(() ->
                        new BusinessException("Collecte introuvable"));

        Map<CategLinge,Integer> compteur = new HashMap<>();




        List<CollecteLinge> linges =
                collecteLingeRepository.findByCollecte_Id(collecteId);

        for (CollecteLinge cl : linges) {

            CategLinge categorie =
                    cl.getLinge().getCategorie();

            compteur.merge(categorie,1,Integer::sum);
        }
        sacInfecteRepository.findByCollecte_Id(collecteId)
                .forEach(sac ->
                        sac.getItems().forEach(item ->
                                compteur.merge(
                                        item.getCategorie(),
                                        item.getQuantiteDeclaree(),
                                        Integer::sum
                                )
                        )
                );
        List<ResumeCategorieDTO> categories =
                compteur.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> ResumeCategorieDTO.builder()
                                .categorie(e.getKey())
                                .quantite(e.getValue())
                                .build())
                        .collect(Collectors.toList());
        int total =
                compteur.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

        return ResumeCollecteDTO.builder()

                .collecteId(collecte.getId())

              //  .dateCollecte(collecte.getDateCollecte())

                .agent(collecte.getAgent().getNom())

                //.serviceId(collecte.getService().getId())

              //  .chambre(collecte.getChambre())
                .categories(categories)
                .totalPieces(total)
                .build();

    }

}