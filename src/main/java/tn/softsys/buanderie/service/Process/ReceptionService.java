package tn.softsys.buanderie.service.Process;

import jakarta.transaction.Transactional;
import lombok.*;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.dto.Reception.ReceptionRFIDRequest;
import tn.softsys.buanderie.dto.Reception.ReceptionRequest;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.management.SacInfecteService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceptionService {
    private final ReceptionBacRepository receptionRepo;
    private final BacRepository bacRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final ServiceRepository serviceRepository;
    private final SacInfecteService sacInfecteService;
     //Réception unifiée : bacs blancs + sacs infectés.
     // Bacs blancs  : vérification classique linge à linge
      //Sacs infectés : réceptionné sans ouverture, numéro enregistré
    @Transactional
    public ReceptionResultDTO recevoirPlusieurs(ReceptionRequest request, Agent agent) {

        List<ReceptionBac> bacsRecus = new ArrayList<>();
        List<SacInfecteDTO> sacsRecus = new ArrayList<>();
        //  Réception des bacs blancs
        if (request.getCodesBac() != null && !request.getCodesBac().isEmpty()) {
            for (String codeBac : request.getCodesBac()) {
                Bac bac = bacRepository.findByCode(codeBac)
                        .orElseThrow(() -> new RuntimeException("Bac introuvable : " + codeBac));
                bac.setEtat(EtatBac.EN_RECEPTION);
                bacRepository.save(bac);
                ReceptionBac reception = ReceptionBac.builder()
                        .codeBac(bac.getCode())
                        //.service(bac.getService())
                        .typeCollecte(bac.getTypeCollecte())
                        .agent(agent)
                        .dateReception(LocalDateTime.now())
                        .build();
                reception = receptionRepo.save(reception);

                List<Linge> linges = lingeRepository.findByBac_Code(codeBac);

                for (Linge linge : linges) {
                    EtatLinge etatAvant = linge.getEtat();
                    linge.setEtat(EtatLinge.RECEPTION);
                    linge.setObservations(
                            "Réception bac " + bac.getCode()
                          );

                    linge.setBac(null);
                    lingeRepository.save(linge);

                    historiqueLingeRepository.save(
                            HistoriqueLinge.builder()
                                    .linge(linge)
                                    .etatApres(EtatLinge.RECEPTION)
                                    .etatAvant(etatAvant)
                                    .agent(agent)
                                    .typeOperation(TypeOperation.RECEPTION_BAC)
                                    .dateOperation(LocalDateTime.now())
                                    .observations("Réception bac " + bac.getCode())
                                    .build());
                }
                bacsRecus.add(reception);
            }
        }
        //Réception des sacs infectés (sans ouverture)
        if (request.getNumerosSacsInfectes() != null && !request.getNumerosSacsInfectes().isEmpty()) {

            for (String numeroSac : request.getNumerosSacsInfectes()) {
                SacInfecteDTO sacDTO = sacInfecteService.recevoirSacInfecte(numeroSac, agent.getMatricule());
                sacsRecus.add(sacDTO);
            }
        }

        return new ReceptionResultDTO(bacsRecus, sacsRecus);
    }
    //methode de reception en mode RFID
    @Transactional
    public ReceptionRFIDResultDTO recevoirRFID(ReceptionRFIDRequest request, Agent agent) {

        Bac bac = bacRepository.findByCode(request.getCodeBac())
                .orElseThrow(() -> new RuntimeException("Bac introuvable"));

        TypeBac typeBac = bac.getTypebac();

        List<Linge> lingesTraites = new ArrayList<>();
    List<Linge> lingesDeBac = lingeRepository.findByBac_Code(request.getCodeBac());
    for (Linge l: lingesDeBac){
        l.setEtat(EtatLinge.RECEPTION);

    }

        for (String tag : request.getRfidTags()) {

            Linge linge = lingeRepository.findByRfidTag(tag);

            if (linge == null) continue;

            EtatLinge ancien = linge.getEtat();

            linge.setEtat(EtatLinge.RECEPTION);

            if (typeBac == TypeBac.BLANC) {
                linge.setTypeCollecte(TypeCollecte.BLANC);
            } else {
                linge.setTypeCollecte(TypeCollecte.INFECTE);
            }
            linge.setBac(null);

            lingeRepository.save(linge);

            historiqueLingeRepository.save(
                    HistoriqueLinge.builder()
                            .linge(linge)
                            .etatAvant(ancien)
                            .etatApres(EtatLinge.RECEPTION)
                            .agent(agent)
                            .typeOperation(TypeOperation.RECEPTION_BAC)
                            .dateOperation(LocalDateTime.now())
                            .observations("RFID portique bac " + bac.getCode())
                            .build()
            );

            lingesTraites.add(linge);
        }

        bac.setEtat(EtatBac.EN_RECEPTION);
        bacRepository.save(bac);

        return new ReceptionRFIDResultDTO(
                lingesTraites,
                bac.getCode(),
                typeBac.name()
        );
    }

     // interne regroupant le résultat de la réception (bacs blancs + sacs infectés).
     @Data @Builder
     @NoArgsConstructor
     @AllArgsConstructor
     public static class ReceptionRFIDResultDTO {

         private List<Linge> lingesTraites;

         private String codeBac;

         private String typeBac;


     }
    public static class ReceptionResultDTO {
        public final List<ReceptionBac> bacsRecus;
        public final List<SacInfecteDTO> sacsInfectesRecus;

        public ReceptionResultDTO(List<ReceptionBac> bacsRecus, List<SacInfecteDTO> sacsInfectesRecus) {
            this.bacsRecus = bacsRecus;
            this.sacsInfectesRecus = sacsInfectesRecus;
        }
    }


}
