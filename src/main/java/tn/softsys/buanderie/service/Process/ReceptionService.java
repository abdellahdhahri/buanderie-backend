package tn.softsys.buanderie.service.Process;

import jakarta.transaction.Transactional;
import lombok.*;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.dto.Reception.ReceptionRFIDRequest;
import tn.softsys.buanderie.dto.Reception.ReceptionRequest;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.management.ProfilExploitationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceptionService {
    private final ReceptionBacRepository receptionRepo;
    private final BacRepository bacRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final ServiceRepository serviceRepository;
    private final ProfilExploitationService profilExploitationService;
    private final PersonnelRepository personnelRepository;





    @Transactional
    public ReceptionResultDTO recevoirPlusieurs(ReceptionRequest request, String code ) {


        Personnel agent = personnelRepository.findByCodeBarre(code);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", code);
        }
        List<ReceptionBac> bacsRecus = new ArrayList<>();
        //  Réception des bacs blancs
        if (request.getCodesBac() != null && !request.getCodesBac().isEmpty()) {
            for (String codeBac : request.getCodesBac()) {
                Bac bac = bacRepository.findByCode(codeBac)
                        .orElseThrow(() -> new RuntimeException("Bac introuvable : " + codeBac));
                boolean infecte = bac.getTypebac() == TypeBac.INFECTE;
                if (infecte) {
                    profilExploitationService.exigerCircuitInfecte();
                }
                bac.setEtat(EtatBac.EN_RECEPTION);
                if (!infecte) {
                    // le poids d'un bac infecté est conservé : il sert au lavage direct
                    bac.setPoids(null);
                }
                bacRepository.save(bac);
                ReceptionBac reception = ReceptionBac.builder()
                        .codeBac(bac.getCode())
                        //.service(bac.getService())
                        .typeCollecte(bac.getTypeCollecte())
                        // .agent(agent)
                        .dateReception(LocalDateTime.now())
                        .build();
                reception = receptionRepo.save(reception);

                List<Linge> linges = lingeRepository.findByBac_Code(codeBac);

                if (infecte) {
                    // Bac infecté : on ne l'ouvre pas et on ne détache pas ses linges.
                    // Il part directement au lavage (désinfection) avec son contenu.
                    if (linges.isEmpty()) {
                        throw new BusinessException(
                                "Le bac infecté " + bac.getCode() + " ne contient aucun linge déclaré.");
                    }
                    for (Linge linge : linges) {
                        historiqueLingeRepository.save(
                                HistoriqueLinge.builder()
                                        .linge(linge)
                                        .etatApres(linge.getEtat())
                                        .etatAvant(linge.getEtat())
                                        .personnelId(agent.getId())
                                        .typeOperation(TypeOperation.RECEPTION_BAC)
                                        .dateOperation(LocalDateTime.now())
                                        .observations("Réception bac infecté " + bac.getCode()
                                                + " — en attente de désinfection")
                                        .build());
                    }
                    bacsRecus.add(reception);
                    continue;
                }

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
                                    .personnelId(agent.getId())
                                    .typeOperation(TypeOperation.RECEPTION_BAC)
                                    .dateOperation(LocalDateTime.now())
                                    .observations("Réception bac " + bac.getCode())
                                    .build());
                }
                bacsRecus.add(reception);
            }
        }
        return new ReceptionResultDTO(bacsRecus);
    }
    //methode de reception en mode RFID
    @Transactional
    public ReceptionRFIDResultDTO recevoirRFID(ReceptionRFIDRequest request, String code) {
        Personnel agent = personnelRepository.findByCodeBarre(code);
        if (agent == null) {
            throw new ResourceNotFoundException("Agent", "matricule", code);
        }

        Bac bac = bacRepository.findByCode(request.getCodeBac())
                .orElseThrow(() -> new RuntimeException("Bac introuvable"));

        TypeBac typeBac = bac.getTypebac();

        if (typeBac == TypeBac.INFECTE) {
            // Bac infecté : réceptionné sans ouverture, linges conservés dans le bac
            profilExploitationService.exigerCircuitInfecte();
            bac.setEtat(EtatBac.EN_RECEPTION);
            bacRepository.save(bac);
            return new ReceptionRFIDResultDTO(new ArrayList<>(), bac.getCode(), typeBac.name());
        }

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
                            .personnelId(agent.getId())
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

    private static final Set<EtatLinge> ETATS_DEJA_EN_BUANDERIE = EnumSet.of(
            EtatLinge.RECEPTION, EtatLinge.EN_TRI, EtatLinge.EN_LAVAGE, EtatLinge.LAVE,
            EtatLinge.EN_SECHAGE, EtatLinge.SECHE, EtatLinge.PLIE, EtatLinge.COLLE,
            EtatLinge.INFECTE_EN_LAVAGE, EtatLinge.INFECTE_LAVE
    );

    @Transactional
    public Map<String, Object> recevoirLingeSansBac(String codeLinge, String matricule, Boolean infecte) {
        Personnel agent = getAgent(matricule);
        Linge linge = rechercherLinge(codeLinge);
        return recevoirSansBac(linge, agent, infecte);
    }

    @Transactional
    public Map<String, Object> recevoirLingesSansBac(List<String> codesLinges, String matricule, Boolean infecte) {
        Personnel agent = getAgent(matricule);

        if (codesLinges == null || codesLinges.isEmpty()) {
            throw new BusinessException("Aucun linge à réceptionner.");
        }

        List<Map<String, Object>> recus = new ArrayList<>();
        List<Map<String, Object>> erreurs = new ArrayList<>();

        for (String code : codesLinges) {
            try {
                recus.add(recevoirSansBac(rechercherLinge(code), agent, infecte));
            } catch (BusinessException e) {
                Map<String, Object> erreur = new LinkedHashMap<>();
                erreur.put("code", code);
                erreur.put("erreur", e.getMessage());
                erreurs.add(erreur);
            }
        }

        Map<String, Map<String, Object>> parService = new LinkedHashMap<>();
        for (Map<String, Object> recu : recus) {
            String cle = String.valueOf(recu.get("serviceId"));
            Map<String, Object> groupe = parService.computeIfAbsent(cle, k -> {
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("serviceId", recu.get("serviceId"));
                g.put("service", recu.get("service"));
                g.put("nbLinges", 0);
                g.put("parCategorie", new LinkedHashMap<String, Integer>());
                return g;
            });
            groupe.put("nbLinges", (Integer) groupe.get("nbLinges") + 1);
            @SuppressWarnings("unchecked")
            Map<String, Integer> parCategorie = (Map<String, Integer>) groupe.get("parCategorie");
            parCategorie.merge(String.valueOf(recu.get("categorie")), 1, Integer::sum);
        }

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("nbRecus", recus.size());
        reponse.put("nbErreurs", erreurs.size());
        reponse.put("parService", new ArrayList<>(parService.values()));
        reponse.put("recus", recus);
        reponse.put("erreurs", erreurs);
        return reponse;
    }

    private Map<String, Object> recevoirSansBac(Linge linge, Personnel agent, Boolean infecte) {

        if (linge.getBac() != null) {
            throw new BusinessException(
                    "Le linge " + linge.getCodeBarre() + " est dans le bac " + linge.getBac().getCode()
                            + " : réceptionnez le bac.");
        }

        if (linge.getEtat() != null && ETATS_DEJA_EN_BUANDERIE.contains(linge.getEtat())) {
            throw new BusinessException(
                    "Le linge " + linge.getCodeBarre() + " est déjà à la buanderie (état : " + linge.getEtat() + ").");
        }

        if (linge.getEtat() == EtatLinge.HORS_SERVICE) {
            throw new BusinessException("Le linge " + linge.getCodeBarre() + " est hors service.");
        }

        boolean estInfecte = Boolean.TRUE.equals(infecte) || linge.getEtat() == EtatLinge.INFECTE_COLLECTE;
        if (estInfecte) {
            profilExploitationService.exigerCircuitInfecte();
        }

        EtatLinge etatAvant = linge.getEtat();
        linge.setEtat(EtatLinge.RECEPTION);
        linge.setTypeCollecte(estInfecte ? TypeCollecte.INFECTE : TypeCollecte.BLANC);
        linge.setLitId(null);
        linge.setChambreId(null);
        linge.setArsenalEmplacementID(null);
        linge.setObservations("Réception sans bac");
        lingeRepository.save(linge);

        historiqueLingeRepository.save(
                HistoriqueLinge.builder()
                        .linge(linge)
                        .etatAvant(etatAvant)
                        .etatApres(EtatLinge.RECEPTION)
                        .personnelId(agent.getId())
                        .typeOperation(TypeOperation.RECEPTION_BAC)
                        .dateOperation(LocalDateTime.now())
                        .observations("Réception sans bac" + (estInfecte ? " — infecté" : ""))
                        .build());

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("lingeId", linge.getId());
        reponse.put("codeBarre", linge.getCodeBarre());
        reponse.put("designation", linge.getDesignation());
        reponse.put("categorie", linge.getCategorie() != null ? linge.getCategorie().name() : null);
        reponse.put("serviceId", linge.getServiceId());
        reponse.put("service", nomService(linge.getServiceId()));
        reponse.put("etatAvant", etatAvant != null ? etatAvant.name() : null);
        reponse.put("typeCollecte", linge.getTypeCollecte().name());
        reponse.put("message", "Linge " + linge.getCodeBarre() + " réceptionné — service "
                + nomService(linge.getServiceId()) + ".");
        return reponse;
    }

    private String nomService(Long serviceId) {
        if (serviceId == null) {
            return "Sans service";
        }
        return serviceRepository.findById(serviceId)
                .map(sv -> sv.getCodeService() != null && !sv.getCodeService().isBlank()
                        ? sv.getCodeService()
                        : sv.getNom())
                .orElse("Service " + serviceId);
    }

    private Personnel getAgent(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            throw new BusinessException("Le matricule de l'agent est obligatoire.");
        }
        Personnel agent = personnelRepository.findByCodeBarre(matricule.trim());
        if (agent == null) {
            throw new BusinessException("Agent introuvable : " + matricule);
        }
        return agent;
    }

    private Linge rechercherLinge(String codeLinge) {
        if (codeLinge == null || codeLinge.isBlank()) {
            throw new BusinessException("Le code du linge est obligatoire.");
        }
        String code = codeLinge.trim();
        return lingeRepository.findByCodeBarre(code)
                .orElseGet(() -> {
                    Linge parTag = lingeRepository.findByRfidTag(code);
                    if (parTag == null) {
                        throw new BusinessException("Linge introuvable : " + code);
                    }
                    return parTag;
                });
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

        public ReceptionResultDTO(List<ReceptionBac> bacsRecus) {
            this.bacsRecus = bacsRecus;
        }
    }


}