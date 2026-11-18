package tn.softsys.buanderie.service.referentiel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ArsenalImportRequest;
import tn.softsys.buanderie.dto.EmplacementDetailDTO;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.ConfigurationIntegration;
import tn.softsys.buanderie.entity.Emplacement;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.ModeSource;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeAuthentification;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ArsenalRepository;
import tn.softsys.buanderie.repository.EmplacementRepository;


@Service
@RequiredArgsConstructor
public class CubixArsenalImportService {

    private static final String SUFFIXE = "/BuanderieApi/Arsenaux";

    private final IntegrationService integrationService;
    private final ConnecteurRest connecteur;
    private final CubixMachineImportService importables;
    private final SynchronisationArsenauxService synchronisationArsenaux;
    private final SynchronisationEmplacementsService synchronisationEmplacements;
    private final ArsenalRepository arsenalRepository;
    private final EmplacementRepository emplacementRepository;

    /** Vrai si l'application travaille en mode CUBIX (sert aux écrans et à la création d'emplacements). */
    public boolean actif() {
        ConfigurationIntegration config = integrationService.getConfiguration();
        return config.getMode() == ModeSource.EXTERNE
                && config.getTypeConnecteur() == TypeConnecteur.CUBIX
                && config.getTypeAuthentification() == TypeAuthentification.CUBIX;
    }

    public JsonNode importables(String q, int page, int pageSize) {
        return importables.importables(q, page, pageSize);
    }

    /** Importe les arsenaux choisis (avec leurs emplacements) puis relit arsenaux et emplacements. */
    public Map<String, Object> importer(ArsenalImportRequest demande) {
        ConfigurationIntegration config = exigerModeCubix();

        if (demande == null || demande.getIds() == null || demande.getIds().isEmpty()) {
            throw new BusinessException("Sélectionnez au moins un arsenal à importer.");
        }

        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        demande.getIds().stream().filter(i -> i != null).distinct().forEach(i -> champs.add("ids", String.valueOf(i)));
        if (demande.getNbrCol() != null) {
            champs.add("nbrCol", String.valueOf(demande.getNbrCol()));
        }
        if (demande.getEmplacements() != null) {
            demande.getEmplacements().forEach(nom -> champs.add("emplacements", nom == null ? "" : nom.trim()));
        }

        JsonNode reponse = connecteur.ecrireFormulaire(baseApi() + "/ImporterArsenaux", config, champs);

        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("importes", ids(reponse.get("importes")));
        resultat.put("ignores", ignores(reponse.get("ignores")));
        resultat.putAll(relire());
        return resultat;
    }

    /** Ajoute des emplacements à un arsenal déjà importé : dans CUBIX d'abord, puis relecture. */
    public Map<String, Object> ajouterEmplacements(Long arsenalId, List<String> noms) {
        ConfigurationIntegration config = exigerModeCubix();

        Arsenal arsenal = arsenalRepository.findById(arsenalId)
                .orElseThrow(() -> new BusinessException("Arsenal introuvable : " + arsenalId));
        if (arsenal.getSource() != SourceDonnee.EXTERNE || arsenal.getIdExterne() == null) {
            throw new BusinessException("Cet arsenal n'est pas un arsenal CUBIX : importez-le d'abord depuis CUBIX.");
        }
        if (noms == null || noms.stream().allMatch(n -> n == null || n.isBlank())) {
            throw new BusinessException("Indiquez au moins un nom d'emplacement.");
        }

        MultiValueMap<String, String> champs = new LinkedMultiValueMap<>();
        champs.add("arsenalId", arsenal.getIdExterne());
        noms.forEach(nom -> champs.add("noms", nom == null ? "" : nom.trim()));

        JsonNode reponse = connecteur.ecrireFormulaire(baseApi() + "/AjouterEmplacements", config, champs);

        Map<String, Object> resultat = new LinkedHashMap<>();
        List<String> crees = new ArrayList<>();
        JsonNode liste = reponse.get("crees");
        if (liste != null && liste.isArray()) {
            liste.forEach(n -> crees.add(n.asText()));
        }
        resultat.put("crees", crees);
        resultat.putAll(relire());
        return resultat;
    }

    /** Création d'un emplacement depuis notre application en mode CUBIX : créé dans CUBIX, puis relu ici. */
    public EmplacementDetailDTO creerEmplacement(EmplacementDetailDTO dto) {
        if (dto.getArsenalId() == null) {
            throw new BusinessException("L'arsenal de l'emplacement est obligatoire.");
        }
        if (dto.getNom() == null || dto.getNom().isBlank()) {
            throw new BusinessException("Le nom de l'emplacement est obligatoire.");
        }
        String nom = dto.getNom().trim();

        Map<String, Object> resultat = ajouterEmplacements(dto.getArsenalId(), List.of(nom));
        @SuppressWarnings("unchecked")
        List<String> crees = (List<String>) resultat.get("crees");
        if (crees == null || crees.isEmpty()) {
            throw new BusinessException("L'emplacement « " + nom + " » existe déjà dans cet arsenal.");
        }

        return emplacementRepository.findByArsenal_IdOrderByNumeroAscNomAsc(dto.getArsenalId()).stream()
                .filter(e -> nom.equalsIgnoreCase(e.getNom()))
                .findFirst()
                .map(EmplacementDetailDTO::depuis)
                .orElseThrow(() -> new BusinessException(
                        "L'emplacement a été créé dans CUBIX mais n'a pas pu être relu : relancez la synchronisation des emplacements."));
    }

    // ------------------------------------------------------------------------------------------

    private Map<String, Object> relire() {
        Map<String, Object> r = new LinkedHashMap<>();
        JournalSynchronisation arsenaux = synchronisationArsenaux.synchroniser("IMPORT_CUBIX");
        JournalSynchronisation emplacements = synchronisationEmplacements.synchroniser("IMPORT_CUBIX");
        r.put("synchronisationArsenaux", arsenaux.getStatut());
        r.put("synchronisationEmplacements", emplacements.getStatut());
        r.put("message", "Arsenaux : " + arsenaux.getMessage() + " Emplacements : " + emplacements.getMessage());
        return r;
    }

    private List<Object> ids(JsonNode noeud) {
        List<Object> ids = new ArrayList<>();
        if (noeud != null && noeud.isArray()) {
            noeud.forEach(n -> ids.add(n.asLong()));
        }
        return ids;
    }

    private List<Object> ignores(JsonNode noeud) {
        List<Object> liste = new ArrayList<>();
        if (noeud != null && noeud.isArray()) {
            noeud.forEach(n -> {
                Map<String, Object> ligne = new LinkedHashMap<>();
                ligne.put("id", n.path("id").asLong());
                ligne.put("raison", n.path("raison").asText());
                liste.add(ligne);
            });
        }
        return liste;
    }

    private ConfigurationIntegration exigerModeCubix() {
        if (!actif()) {
            throw new BusinessException(
                    "Cette opération n'est disponible qu'en mode externe, connecteur CUBIX, authentification CUBIX.");
        }
        return integrationService.getConfiguration();
    }

    private String baseApi() {
        String url = integrationService.urlComplete(Referentiel.ARSENAL);
        int fin = url.indexOf('?');
        String sansParametres = fin >= 0 ? url.substring(0, fin) : url;
        if (!sansParametres.toLowerCase().endsWith(SUFFIXE.toLowerCase())) {
            throw new BusinessException("L'URL configurée pour les arsenaux (" + sansParametres + ") n'est pas celle de la "
                    + "nouvelle API CUBIX : elle doit se terminer par « " + SUFFIXE
                    + " ». Réappliquez le modèle CUBIX (POST /api/integration/modele/CUBIX).");
        }
        return sansParametres.substring(0, sansParametres.length() - "/Arsenaux".length());
    }
}
