package tn.softsys.buanderie.service.Process;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;
import tn.softsys.buanderie.dto.StockageRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockageService {
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final SqlServerServiceRepository serviceRepository;
    private final AgentRepository agentRepository;
    private final ArsenalRepository arsenalRepository;
    private final NotificationService notificationService;
    private final SqlServerPersonnelRepository sqlPersonnelRepo;




    @Transactional
    public Linge Stocker(StockageRequest request) {

        //recuperer l'agent
        Personnel agent = sqlPersonnelRepo.findByCodeBarre(request.getMatriculeAgent());
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Agent",
//                                "matricule",
//                                request.getMatriculeAgent()));
        //recuperer linge
        Linge linge = rechercherLinge(request.getCodeOuTag());
        Arsenal  arsenal = getLingeEmplacement(linge.getCodeBarre());
        if (linge.getEtat() != EtatLinge.PLIE) {
            throw new BusinessException(
                    "Le linge doit être en état SECHE.");
        }

        EtatLinge ancienEtat = linge.getEtat();

        linge.setEtat(EtatLinge.STOCKE);

        linge.setEmplacement(arsenal);

        linge = lingeRepository.save(linge);
        if(linge.getCategorie()==CategLinge.UNIFORME){

            notificationService.notifierUniformeStocke(linge);
        }
        Long service = linge.getServiceId();
        linge.setEmplacement(arsenal);
        creerHistorique(
                linge,
                agent.getId(),
                ancienEtat,
                EtatLinge.STOCKE,
                "Stocké dans l'armoire du  "+arsenal.getNom()
        );


        return linge;


    }


    private Linge rechercherLinge(String codeOuTag) {

        Optional<Linge> linge = lingeRepository.findByCodeBarre(codeOuTag);

        if (linge.isPresent()) {
            return linge.get();
        }
        Linge lingeRFID = lingeRepository.findByRfidTag(codeOuTag);

        if (lingeRFID != null) {
            return lingeRFID;
        }

        throw new ResourceNotFoundException(
                "Linge",
                "codeBarre/rfidTag",
                codeOuTag);
    }

    //methode pour creer le historique d'une linge
    private void creerHistorique(
            Linge linge,
            Long id,
            EtatLinge avant,
            EtatLinge apres,
            String observation) {

        historiqueLingeRepository.save(

                HistoriqueLinge.builder()
                        .linge(linge)
                        .personnelId(id)
                        .etatAvant(avant)
                        .etatApres(apres)
                        .typeOperation(TypeOperation.MISE_EN_STOCKAGE)
                        .dateOperation(LocalDateTime.now())
                        .observations(observation)
                        .build()
        );
    }

  /*  public Linge  verifierLinge(String code){
        Optional<Linge> linge = lingeRepository.findByCodeBarre(code);





    }*/

    public Arsenal getLingeEmplacement(String code){

        Linge linge = lingeRepository.findByCodeBarre(code)
                .orElseThrow(() ->
                        new RuntimeException("Linge introuvable : " + code)
                );

        Long serviceId = linge.getServiceId();

        Arsenal arsenal = arsenalRepository.findByServiceId(serviceId);

        if (arsenal == null) {
            throw new RuntimeException(
                    "Aucun arsenal trouvé pour le service ID : " + serviceId
            );
        }

        return arsenal;
    }
    public List<Linge> getLingesByArsenal(Long arsenalId) {

        return lingeRepository.findByEmplacement_Id(arsenalId);

    }
}
