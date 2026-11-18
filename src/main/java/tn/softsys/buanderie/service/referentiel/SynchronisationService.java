package tn.softsys.buanderie.service.referentiel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class SynchronisationService {

    private final IntegrationService integrationService;
    private final SynchronisationPersonnelService synchronisationPersonnelService;
    private final SynchronisationServicesService synchronisationServicesService;
    private final SynchronisationLingesService synchronisationLingesService;
    private final SynchronisationMachinesService synchronisationMachinesService;
    private final SynchronisationArsenauxService synchronisationArsenauxService;
    private final SynchronisationChambresService synchronisationChambresService;
    private final SynchronisationLitsService synchronisationLitsService;
    private final SynchronisationLitsInfectesService synchronisationLitsInfectesService;
    private final SynchronisationEmplacementsService synchronisationEmplacementsService;
    private final SynchronisationBacsService synchronisationBacsService;

    public JournalSynchronisation synchroniser(Referentiel referentiel, String declencheur) {
        return switch (referentiel) {
            case SERVICE -> synchronisationServicesService.synchroniser(declencheur);
            case PERSONNEL -> synchronisationPersonnelService.synchroniser(declencheur);
            case LINGE -> synchronisationLingesService.synchroniser(declencheur);
            case MACHINE -> synchronisationMachinesService.synchroniser(declencheur);
            case ARSENAL -> synchronisationArsenauxService.synchroniser(declencheur);
            case CHAMBRE -> synchronisationChambresService.synchroniser(declencheur);
            case LIT -> synchronisationLitsService.synchroniser(declencheur);
            case LIT_INFECTE -> synchronisationLitsInfectesService.synchroniser(declencheur);
            case EMPLACEMENT -> synchronisationEmplacementsService.synchroniser(declencheur);
            case BAC -> synchronisationBacsService.synchroniser(declencheur);
            default -> throw new BusinessException(
                    "La synchronisation de " + referentiel + " n'est pas encore disponible.");
        };
    }

    public List<JournalSynchronisation> synchroniserTout(String declencheur) {
        List<JournalSynchronisation> resultats = new ArrayList<>();
        for (EndpointIntegration endpoint : integrationService.getEndpoints()) {
            if (Boolean.TRUE.equals(endpoint.getActif())
                    && endpoint.getReferentiel().isSynchronisationDisponible()
                    && integrationService.estImporte(endpoint.getReferentiel())) {
                resultats.add(synchroniser(endpoint.getReferentiel(), declencheur));
            }
        }
        return resultats;
    }

    public Map<String, Object> tester(Referentiel referentiel) {
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("referentiel", referentiel.name());
        try {
            resultat.put("url", integrationService.urlComplete(referentiel));
            int nombre = switch (referentiel) {
                case SERVICE -> synchronisationServicesService.tester();
                case PERSONNEL -> synchronisationPersonnelService.tester();
                case LINGE -> synchronisationLingesService.tester();
                case MACHINE -> synchronisationMachinesService.tester();
                case ARSENAL -> synchronisationArsenauxService.tester();
                case CHAMBRE -> synchronisationChambresService.tester();
                case LIT -> synchronisationLitsService.tester();
                case LIT_INFECTE -> synchronisationLitsInfectesService.tester();
                case EMPLACEMENT -> synchronisationEmplacementsService.tester();
                case BAC -> synchronisationBacsService.tester();
                default -> throw new BusinessException(
                        "Le test de " + referentiel + " n'est pas encore disponible.");
            };
            resultat.put("succes", true);
            resultat.put("nombreElements", nombre);
            resultat.put("message", nombre + " élément(s) reçu(s).");
        } catch (Exception e) {
            resultat.put("succes", false);
            resultat.put("message", e.getMessage());
        }
        return resultat;
    }
}