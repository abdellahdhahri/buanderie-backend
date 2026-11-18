package tn.softsys.buanderie.service.Reporting;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatImmobilisation;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.ZoneStock;
import tn.softsys.buanderie.repository.ServiceRepository;
import tn.softsys.buanderie.repository.SuiviStockRepository;

@Service
@RequiredArgsConstructor
public class SuiviStockService {

    private static final List<EtatImmobilisation> SORTIS_DU_PARC = List.of(
            EtatImmobilisation.VENDU,
            EtatImmobilisation.PERDU,
            EtatImmobilisation.MISE_AU_REBUT,
            EtatImmobilisation.VOLE,
            EtatImmobilisation.ANNULE
    );

    private static final Set<EtatLinge> ETATS_SALE = EnumSet.of(
            EtatLinge.SALE, EtatLinge.COLLECTE, EtatLinge.INFECTE_COLLECTE
    );

    private static final Set<EtatLinge> ETATS_CIRCUIT = EnumSet.of(
            EtatLinge.RECEPTION, EtatLinge.EN_TRI, EtatLinge.EN_LAVAGE, EtatLinge.LAVE,
            EtatLinge.EN_SECHAGE, EtatLinge.SECHE, EtatLinge.PLIE, EtatLinge.COLLE,
            EtatLinge.INFECTE_EN_LAVAGE, EtatLinge.INFECTE_LAVE, EtatLinge.INFECTE_ECART
    );

    private static final Set<EtatLinge> ETATS_PERTES = EnumSet.of(
            EtatLinge.MANQUANT, EtatLinge.HORS_SERVICE
    );

    private final SuiviStockRepository suiviStockRepository;
    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSuiviGlobal() {
        List<Ligne> lignes = chargerLignes();
        Map<Long, String> nomsServices = nomsServices();

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("dateCalcul", LocalDateTime.now());
        reponse.put("totalParc", total(lignes));
        reponse.put("parZone", parZone(lignes));
        reponse.put("parCategorie", parCategorieEtZone(lignes));

        List<Map<String, Object>> services = new ArrayList<>();
        lignes.stream()
                .collect(Collectors.groupingBy(l -> l.serviceId == null ? -1L : l.serviceId, TreeMap::new, Collectors.toList()))
                .forEach((serviceId, contenu) -> {
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("serviceId", serviceId < 0 ? null : serviceId);
                    s.put("service", serviceId < 0 ? "Sans service" : nomsServices.getOrDefault(serviceId, "Service " + serviceId));
                    s.put("totalParc", total(contenu));
                    s.put("parZone", parZone(contenu));
                    services.add(s);
                });
        reponse.put("services", services);
        return reponse;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSuiviService(Long serviceId) {
        List<Ligne> lignes = chargerLignes().stream()
                .filter(l -> Objects.equals(l.serviceId, serviceId))
                .toList();

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("dateCalcul", LocalDateTime.now());
        reponse.put("serviceId", serviceId);
        reponse.put("service", nomsServices().getOrDefault(serviceId, "Service " + serviceId));
        reponse.put("totalParc", total(lignes));
        reponse.put("parZone", parZone(lignes));
        reponse.put("parCategorie", parCategorieEtZone(lignes));
        return reponse;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLingesParZone(ZoneStock zone, Long serviceId, CategLinge categorie, int page, int size) {
        Set<EtatLinge> etats = etatsDeLaZone(zone);

        List<Linge> source = serviceId == null
                ? suiviStockRepository.findLingesParEtats(etats, SORTIS_DU_PARC)
                : suiviStockRepository.findLingesParEtatsEtService(etats, serviceId, SORTIS_DU_PARC);

        List<Map<String, Object>> linges = source
                .stream()
                .filter(l -> zone(l.getEtat(), l.getArsenalEmplacementID() != null,
                        l.getLitId() != null || l.getChambreId() != null,
                        l.getPorteurId() != null) == zone)
                .filter(l -> categorie == null || l.getCategorie() == categorie)
                .map(this::versLigneLinge)
                .toList();

        int taille = Math.max(1, Math.min(size, 500));
        int debut = Math.max(0, page) * taille;
        int fin = Math.min(debut + taille, linges.size());

        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("zone", zone.name());
        reponse.put("serviceId", serviceId);
        reponse.put("categorie", categorie != null ? categorie.name() : null);
        reponse.put("total", linges.size());
        reponse.put("page", Math.max(0, page));
        reponse.put("size", taille);
        reponse.put("totalPages", (int) Math.ceil(linges.size() / (double) taille));
        reponse.put("linges", debut < linges.size() ? linges.subList(debut, fin) : List.of());
        return reponse;
    }

    private List<Ligne> chargerLignes() {
        List<Ligne> lignes = new ArrayList<>();
        for (Object[] row : suiviStockRepository.agregerParc(SORTIS_DU_PARC)) {
            Ligne l = new Ligne();
            l.serviceId = (Long) row[0];
            l.categorie = (CategLinge) row[1];
            EtatLinge etat = (EtatLinge) row[2];
            boolean enArsenal = ((Number) row[3]).intValue() == 1;
            boolean surLit = ((Number) row[4]).intValue() == 1;
            boolean avecPorteur = ((Number) row[5]).intValue() == 1;
            l.zone = zone(etat, enArsenal, surLit, avecPorteur);
            l.quantite = ((Number) row[6]).longValue();
            lignes.add(l);
        }
        return lignes;
    }

    private ZoneStock zone(EtatLinge etat, boolean enArsenal, boolean surLit, boolean avecPorteur) {
        if (etat == null) {
            return ZoneStock.STOCK_NON_RANGE;
        }
        if (etat == EtatLinge.STOCKE) {
            return enArsenal ? ZoneStock.OFFICE : ZoneStock.STOCK_NON_RANGE;
        }
        if (etat == EtatLinge.LIVRE || etat == EtatLinge.PROPRE) {
            if (avecPorteur) {
                return ZoneStock.PORTEURS;
            }
            if (surLit || etat == EtatLinge.PROPRE) {
                return ZoneStock.CHAMBRES;
            }
            return ZoneStock.A_RANGER;
        }
        if (ETATS_SALE.contains(etat)) {
            return ZoneStock.SALE;
        }
        if (ETATS_CIRCUIT.contains(etat)) {
            return ZoneStock.CIRCUIT_BUANDERIE;
        }
        if (ETATS_PERTES.contains(etat)) {
            return ZoneStock.PERTES;
        }
        return ZoneStock.CIRCUIT_BUANDERIE;
    }

    private Set<EtatLinge> etatsDeLaZone(ZoneStock zone) {
        return switch (zone) {
            case OFFICE, STOCK_NON_RANGE -> EnumSet.of(EtatLinge.STOCKE);
            case CHAMBRES -> EnumSet.of(EtatLinge.LIVRE, EtatLinge.PROPRE);
            case PORTEURS, A_RANGER -> EnumSet.of(EtatLinge.LIVRE);
            case SALE -> ETATS_SALE;
            case CIRCUIT_BUANDERIE -> ETATS_CIRCUIT;
            case PERTES -> ETATS_PERTES;
        };
    }

    private Map<String, Long> parZone(List<Ligne> lignes) {
        Map<String, Long> resultat = new LinkedHashMap<>();
        for (ZoneStock z : ZoneStock.values()) {
            resultat.put(z.name(), 0L);
        }
        lignes.forEach(l -> resultat.merge(l.zone.name(), l.quantite, Long::sum));
        return resultat;
    }

    private List<Map<String, Object>> parCategorieEtZone(List<Ligne> lignes) {
        Map<String, List<Ligne>> parCategorie = lignes.stream()
                .collect(Collectors.groupingBy(
                        l -> l.categorie != null ? l.categorie.name() : "AUTRE",
                        TreeMap::new,
                        Collectors.toList()));

        List<Map<String, Object>> resultat = new ArrayList<>();
        parCategorie.forEach((categorie, contenu) -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("categorie", categorie);
            ligne.put("total", total(contenu));
            ligne.put("parZone", parZone(contenu));
            resultat.add(ligne);
        });
        return resultat;
    }

    private long total(List<Ligne> lignes) {
        return lignes.stream().mapToLong(l -> l.quantite).sum();
    }

    private Map<Long, String> nomsServices() {
        Map<Long, String> noms = new HashMap<>();
        for (ServiceEntity s : serviceRepository.findAll()) {
            noms.put(s.getId(), s.getCodeService() != null && !s.getCodeService().isBlank()
                    ? s.getCodeService()
                    : s.getNom());
        }
        return noms;
    }

    private Map<String, Object> versLigneLinge(Linge l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lingeId", l.getId());
        m.put("codeBarre", l.getCodeBarre());
        m.put("designation", l.getDesignation());
        m.put("categorie", l.getCategorie() != null ? l.getCategorie().name() : null);
        m.put("etat", l.getEtat() != null ? l.getEtat().name() : null);
        m.put("serviceId", l.getServiceId());
        m.put("arsenalEmplacementID", l.getArsenalEmplacementID());
        m.put("chambreId", l.getChambreId());
        m.put("litId", l.getLitId());
        m.put("porteurNom", l.getPorteurNom());
        return m;
    }

    private static class Ligne {
        private Long serviceId;
        private CategLinge categorie;
        private ZoneStock zone;
        private long quantite;
    }
}
