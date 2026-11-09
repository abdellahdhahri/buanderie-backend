package tn.softsys.buanderie.service.Reporting;

import tn.softsys.buanderie.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.repository.*;
import tn.softsys.buanderie.service.management.OperationsEtape;
import tn.softsys.buanderie.service.management.ParcoursService;
import tn.softsys.buanderie.service.management.ProfilExploitationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalytiqueDashboardService {

    private final LingeRepository            lingeRepo;
    private final MachineRepository          machineRepo;
    private final CollecteRepository         collecteRepo;
    private final CycleLavageRepository      cycleLavageRepo;
    private final CycleSechageRepository     cycleSechageRepo;
    private final SacInfecteRepository       sacInfecteRepo;
    private final HistoriqueLingeRepository  historiqueRepo;
    private final ServiceRepository sqlServiceRepo;
    private final NotificationService notificationService;
    private final ParcoursService parcoursService;
    private final ProfilExploitationService profilExploitationService;

    private static final int    SEUIL_CRITIQUE  = 3;
    private static final int    SEUIL_ATTENTION = 8;
    private static final int    JOURS_BLOCAGE   = 3;
    private static final double SEUIL_FIN_VIE   = 0.90;
    private static final int    HEURES_SAC      = 48;


    public Map<String, Object> getDashboardComplet() {
        var linges   = lingeRepo.findAll();
        var machines = machineRepo.findAll();
        var debut    = LocalDate.now().atStartOfDay();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpis",              buildKpis(linges, machines, debut));
        result.put("distributionEtats", buildDistribution(linges));
        result.put("stockParService",   buildStockParService(linges));
        result.put("alertes",           buildAlertes(linges));
        result.put("parcours",          buildParcours(linges));
        result.put("machines",          buildMachines(machines, debut));
        result.put("activiteAgents",    buildAgents(debut));
        return result;
    }


    public Map<String, Object> getKpis() {
        return buildKpis(lingeRepo.findAll(), machineRepo.findAll(),
                LocalDate.now().atStartOfDay());
    }

    private Map<String, Object> buildKpis(
            List<tn.softsys.buanderie.entity.Linge> linges,
            List<tn.softsys.buanderie.entity.Machine> machines,
            LocalDateTime debutJour) {

        long total   = linges.size();
        long actifs  = linges.stream()
                .filter(l -> l.getEtat() != EtatLinge.HORS_SERVICE).count();

        Set<EtatLinge> enCircuit = etatsEnCircuit();

        Set<EtatLinge> etatsDisponibles = etatsDisponibles();

        long circuit     = linges.stream().filter(l -> enCircuit.contains(l.getEtat())).count();
        long disponibles = linges.stream()
                .filter(l -> l.getEtat() == EtatLinge.LIVRE || etatsDisponibles.contains(l.getEtat()))
                .count();

        long collectesJ = collecteRepo.findAll().stream()
                .filter(c -> c.getDateCollecte() != null && c.getDateCollecte().isAfter(debutJour))
                .count();

        long lavagesJ = cycleLavageRepo.findAll().stream()
                .filter(c -> c.getStatut() == StatutCycle.TERMINE
                        && c.getDateFin() != null && c.getDateFin().isAfter(debutJour))
                .count();

        long enCours = machines.stream()
                .filter(m -> "EN_COURS".equals(
                        m.getEtatImmobilisation() != null ? m.getEtatImmobilisation().name() : "")).count();
        double tauxMachines = machines.isEmpty() ? 0
                : round((double) enCours / machines.size() * 100);

        long sacsAtt = sacInfecteRepo.findAll().stream()
                .filter(s -> s.getEtat() == EtatSac.DECLARE
                        || s.getEtat() == EtatSac.RECEPTIONNE)
                .count();

        long procheFin = linges.stream()
                .filter(l -> l.getDureVie() != null && l.getDureVie() > 0
                        && (double) l.getNbLavage() / l.getDureVie() >= SEUIL_FIN_VIE)
                .count();

        long totalCollecte = linges.stream()
                .filter(l -> l.getTypeCollecte() != null).count();
        long infectes = linges.stream()
                .filter(l -> l.getTypeCollecte() == TypeCollecte.INFECTE).count();
        double tauxInf = totalCollecte > 0 ? round((double) infectes / totalCollecte * 100) : 0;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalLinges",             total);
        r.put("lingesActifs",            actifs);
        r.put("lingesEnCircuit",         circuit);
        r.put("lingesDisponibles",       disponibles);
        r.put("collectesAujourdhui",     collectesJ);
        r.put("lavagesAujourdhui",       lavagesJ);
        r.put("tauxOccupationMachines",  tauxMachines);
        r.put("sacsInfectesEnAttente",   profilExploitationService.circuitInfecteActif() ? sacsAtt : 0L);
        r.put("circuitInfecteActif",     profilExploitationService.circuitInfecteActif());
        r.put("lingesProcheFin",         procheFin);
        r.put("tauxInfectionMois",       profilExploitationService.circuitInfecteActif() ? tauxInf : 0.0);
        return r;
    }

    public Map<String, Object> getDistribution() {
        return buildDistribution(lingeRepo.findAll());
    }

    private Map<String, Object> buildDistribution(
            List<tn.softsys.buanderie.entity.Linge> linges) {

        Map<EtatLinge, Long> counts = linges.stream()
                .collect(Collectors.groupingBy(
                        tn.softsys.buanderie.entity.Linge::getEtat,
                        Collectors.counting()));

        long total = linges.isEmpty() ? 1 : linges.size();

        Map<String, Object> r = new LinkedHashMap<>();
        for (EtatLinge e : EtatLinge.values()) {
            long v = counts.getOrDefault(e, 0L);
            r.put(e.name().toLowerCase(), v);
        }
        // pourcentages
        Map<String, Double> pct = new LinkedHashMap<>();
        counts.forEach((e, v) -> pct.put(e.name(), round((double) v / total * 100)));
        r.put("pourcentages", pct);
        return r;
    }


    public List<Map<String, Object>> getStockParService() {
        return buildStockParService(lingeRepo.findAll());
    }

    private List<Map<String, Object>> buildStockParService(
            List<tn.softsys.buanderie.entity.Linge> linges) {

        Map<Long, List<tn.softsys.buanderie.entity.Linge>> parService = linges.stream()
                .filter(l -> l.getServiceId() != null)
                .collect(Collectors.groupingBy(
                        tn.softsys.buanderie.entity.Linge::getServiceId));

        Set<EtatLinge> circuit = etatsEnCircuit();

        Set<EtatLinge> etatsDisponibles = etatsDisponibles();

        return sqlServiceRepo.findAll().stream().map(svc -> {
                    var ls = parService.getOrDefault(svc.getId(), List.of());

                    long nbLivre   = ls.stream().filter(l -> l.getEtat() == EtatLinge.LIVRE).count();
                    long nbStocke  = ls.stream().filter(l -> etatsDisponibles.contains(l.getEtat())).count();
                    long nbCircuit = ls.stream().filter(l -> circuit.contains(l.getEtat())).count();
                    long nbTotal   = ls.size();

                    String statut;
                    if      (nbLivre + nbStocke <= SEUIL_CRITIQUE)  statut = "CRITIQUE";
                    else if (nbLivre + nbStocke <= SEUIL_ATTENTION) statut = "ATTENTION";
                    else                                              statut = "NORMAL";

                    Long joursRupture = null;
                    if (nbCircuit > 0 && nbLivre + nbStocke < SEUIL_ATTENTION && nbTotal > 0) {
                        double usage = Math.max(1, nbTotal * 0.15);
                        joursRupture = Math.round((nbLivre + nbStocke) / usage);
                    }

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("serviceId",               svc.getId());
                    m.put("serviceCode",             svc.getCode());
                    m.put("serviceNom",              svc.getDescription() != null
                            ? svc.getDescription() : svc.getCode());
                    m.put("nbLivre",                 nbLivre);
                    m.put("nbStocke",                nbStocke);
                    m.put("nbEnCircuit",             nbCircuit);
                    m.put("nbTotal",                 nbTotal);
                    m.put("statut",                  statut);
                    m.put("joursAvantRupturePrevue", joursRupture);
                    return m;
                })
                .sorted(Comparator.comparing(m -> {
                    String s = (String) m.get("statut");
                    return "CRITIQUE".equals(s) ? 0 : "ATTENTION".equals(s) ? 1 : 2;
                }))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getAlertes() {
        return buildAlertes(lingeRepo.findAll());
    }

    private Map<String, Object> buildAlertes(
            List<tn.softsys.buanderie.entity.Linge> linges) {

        // A) Stock critique
        List<Map<String, Object>> alertesStock = buildStockParService(linges).stream()
                .filter(s -> !"NORMAL".equals(s.get("statut")))
                .map(s -> alerteMap(
                        "STOCK_" + s.get("statut"),
                        "CRITIQUE".equals(s.get("statut")) ? "CRITICAL" : "WARNING",
                        "Stock " + s.get("statut").toString().toLowerCase()
                                + " → " + s.get("serviceCode"),
                        s.get("serviceCode").toString(),
                        s.get("nbLivre") + " en place, " + s.get("nbStocke") + " en stock"
                                + (s.get("joursAvantRupturePrevue") != null
                                ? " — rupture ~" + s.get("joursAvantRupturePrevue") + "j" : "")))
                .collect(Collectors.toList());

        //  Linges bloqués
        List<Map<String, Object>> alertesBloquees = new ArrayList<>();
        try {
            LocalDateTime seuil = LocalDateTime.now().minusDays(JOURS_BLOCAGE);

            //  Récupère tous les historiques bloqués avant le seuil
            List<tn.softsys.buanderie.entity.HistoriqueLinge> historiques =
                    historiqueRepo.findHistoriquesBloqués(seuil);

            //  Garder seulement le plus récent par linge
            //    (évite les doublons si un linge a plusieurs entrées bloquées)
            Map<Long, tn.softsys.buanderie.entity.HistoriqueLinge> parLinge =
                    new LinkedHashMap<>();
            for (var h : historiques) {
                if (h.getLinge() == null) continue;
                Long lid = h.getLinge().getId();
                // findHistoriquesBloqués retourne DESC → premier = le plus récent
                parLinge.putIfAbsent(lid, h);
            }

            //  Vérifier que le linge est encore dans cet état bloqué aujourd'hui
            parLinge.values().stream().limit(10).forEach(h -> {
                var linge = h.getLinge();
                // Si l'état actuel du linge est toujours COLLECTE/RECEPTION/EN_TRI
                if (linge.getEtat() == EtatLinge.COLLECTE
                        || linge.getEtat() == EtatLinge.RECEPTION
                        || linge.getEtat() == EtatLinge.EN_TRI) {
                    alertesBloquees.add(alerteMap(
                            "LINGE_BLOQUE", "WARNING",
                            "Bloqué en " + linge.getEtat().name()
                                    + " depuis +" + JOURS_BLOCAGE + "j",
                            linge.getCodeBarre(),
                            linge.getCategorie() != null
                                    ? linge.getCategorie().name() : ""));
                }
            });
        } catch (Exception ignored) {}

        //  Fin de vie
        List<Map<String, Object>> alertesFinVie = linges.stream()
                .filter(l -> l.getDureVie() != null && l.getDureVie() > 0
                        && (double) l.getNbLavage() / l.getDureVie() >= SEUIL_FIN_VIE)
                .limit(20)
                .map(l -> {
                    double usure = (double) l.getNbLavage() / l.getDureVie() * 100;
                    return alerteMap(
                            "FIN_VIE",
                            usure >= 100 ? "CRITICAL" : "WARNING",
                            (usure >= 100 ? "Hors service" : "Proche fin de vie")
                                    + " (" + (int) usure + "%)",
                            l.getCodeBarre(),
                            (l.getCategorie() != null ? l.getCategorie().name() : "")
                                    + " — " + l.getNbLavage() + "/" + l.getDureVie());
                })
                .collect(Collectors.toList());

        //  Sacs en attente
        List<Map<String, Object>> alertesSacs = !profilExploitationService.circuitInfecteActif()
                ? List.<Map<String, Object>>of()
                : sacInfecteRepo.findAll().stream()
                  .filter(s -> (s.getEtat() == EtatSac.DECLARE
                                || s.getEtat() == EtatSac.RECEPTIONNE)
                               && s.getDateCollecte() != null
                               && s.getDateCollecte().isBefore(
                          LocalDateTime.now().minusHours(HEURES_SAC)))
                  .map(s -> alerteMap(
                          "SAC_ATTENTE", "CRITICAL",
                          "Sac infecté en attente depuis +" + HEURES_SAC + "h",
                          s.getNumeroSac(),
                          "État : " + s.getEtat().name()
                          + " — " + s.getQuantiteDeclaree() + " linge(s)"))
                  .collect(Collectors.toList());

        int total = alertesStock.size() + alertesBloquees.size()
                + alertesFinVie.size() + alertesSacs.size();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("stockCritique",  alertesStock);
        r.put("lingesBloqués",  alertesBloquees);
        r.put("finDeVie",       alertesFinVie);
        r.put("sacsEnAttente",  alertesSacs);
        r.put("totalAlertes",   total);
        return r;
    }


    private List<Map<String, Object>> buildMachines(
            List<tn.softsys.buanderie.entity.Machine> machines,
            LocalDateTime debutJour) {

        Map<Long, Long> cyclesLav = cycleLavageRepo.findAll().stream()
                .filter(c -> c.getDateDebut() != null
                        && c.getDateDebut().isAfter(debutJour)
                        && c.getMachine() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getMachine().getId(), Collectors.counting()));

        Map<Long, Double> poidsLav = cycleLavageRepo.findAll().stream()
                .filter(c -> c.getDateDebut() != null
                        && c.getDateDebut().isAfter(debutJour)
                        && c.getMachine() != null && c.getPoidsCharge() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getMachine().getId(),
                        Collectors.summingDouble(
                                tn.softsys.buanderie.entity.CycleLavage::getPoidsCharge)));

        Map<Long, Long> cyclesSec = cycleSechageRepo.findAll().stream()
                .filter(c -> c.getDateDebut() != null
                        && c.getDateDebut().isAfter(debutJour)
                        && c.getMachine() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getMachine().getId(), Collectors.counting()));

        Map<Long, Double> poidsSec = cycleSechageRepo.findAll().stream()
                .filter(c -> c.getDateDebut() != null
                        && c.getDateDebut().isAfter(debutJour)
                        && c.getMachine() != null && c.getPoidsCharge() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getMachine().getId(),
                        Collectors.summingDouble(
                                tn.softsys.buanderie.entity.CycleSechage::getPoidsCharge)));

        return machines.stream().map(m -> {
                    long   cycles = cyclesLav.getOrDefault(m.getId(), 0L)
                            + cyclesSec.getOrDefault(m.getId(), 0L);
                    double poids  = poidsLav.getOrDefault(m.getId(), 0.0)
                            + poidsSec.getOrDefault(m.getId(), 0.0);

                    Map<String, Object> mp = new LinkedHashMap<>();
                    mp.put("id",                     m.getId());
                    mp.put("code",                   m.getCode());
                    mp.put("type",                   m.getType() != null ? m.getType().name() : null);
                    mp.put("etat",                   m.getEtatImmobilisation() != null ? m.getEtatImmobilisation().name() : null);
                    mp.put("capaciteKg",             m.getCapaciteKg());
                    mp.put("cyclesAujourdhui",       (int) cycles);
                    mp.put("poidsTraiteAujourdhui",  round(poids));
                    return mp;
                })
                .sorted(Comparator.comparing(m -> {
                    String e = (String) m.get("etat");
                    return "EN_COURS".equals(e) ? 0 : "DISPONIBLE".equals(e) ? 1 : 2;
                }))
                .collect(Collectors.toList());
    }


    private List<Map<String, Object>> buildAgents(LocalDateTime debutJour) {
        try {
            return historiqueRepo.activiteAgentsAujourdhui(debutJour)
                    .stream()
                    .map(row -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("matricule",        row[0]);
                        m.put("nomComplet",       row[1] + " " + row[2]);
                        m.put("nbOperations",     ((Number) row[3]).longValue());
                        m.put("derniereOperation",row[4] != null ? row[4].toString() : "");
                        m.put("etapeActive",      inferEtape(row[4] != null
                                ? row[4].toString() : ""));
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCHEDULERS
    // ─────────────────────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 300_000)
    public void verifierAlertesCritiques() {

        try {

            LocalDateTime seuil =
                    LocalDateTime.now().minusDays(JOURS_BLOCAGE);

            List<HistoriqueLinge> historiques =
                    historiqueRepo.findHistoriquesBloqués(seuil);

            Map<Long, HistoriqueLinge> parLinge =
                    new LinkedHashMap<>();

            for (HistoriqueLinge h : historiques) {

                if (h.getLinge() == null) {
                    continue;
                }

                Long lingeId = h.getLinge().getId();

                // Le repository retourne normalement DESC
                parLinge.putIfAbsent(lingeId, h);
            }

            int notificationsEnvoyees = 0;

            for (HistoriqueLinge h : parLinge.values()) {

                Linge linge = h.getLinge();

                // Vérifier que le linge est toujours bloqué
                if (!estEtatBloque(linge.getEtat())) {
                    continue;
                }

                long joursBloque =
                        java.time.Duration
                                .between(
                                        h.getDateOperation(),
                                        LocalDateTime.now()
                                )
                                .toDays();

                notificationService.notifierLingeBloque(
                        linge,
                        joursBloque
                );

                notificationsEnvoyees++;
            }

            System.out.println(
                    "[SMART] Vérification linges bloqués : "
                            + notificationsEnvoyees
                            + " linge(s)"
            );

        } catch (Exception e) {

            System.err.println(
                    "[SMART] Erreur vérification linges bloqués : "
                            + e.getMessage()
            );
        }
    }


    private boolean estEtatBloque(EtatLinge etat) {

        return etat == EtatLinge.COLLECTE
                || etat == EtatLinge.RECEPTION
                || etat == EtatLinge.EN_TRI;
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRES PRIVÉS
    // ─────────────────────────────────────────────────────────────────────
    private Map<String, Object> alerteMap(String type, String niveau,
                                          String message, String ref, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type",    type);
        m.put("niveau",  niveau);
        m.put("message", message);
        m.put("reference", ref);
        m.put("detail",  detail);
        return m;
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private Set<EtatLinge> etatsDisponibles() {
        Set<EtatLinge> etats = parcoursService.etatsAcceptes(CodeEtape.DISTRIBUTION);
        return etats.isEmpty() ? Set.of(EtatLinge.STOCKE) : etats;
    }

    private Set<EtatLinge> etatsEnCircuit() {
        Set<EtatLinge> circuit = new LinkedHashSet<>(List.of(
                EtatLinge.COLLECTE, EtatLinge.RECEPTION, EtatLinge.EN_TRI,
                EtatLinge.EN_LAVAGE, EtatLinge.LAVE,
                EtatLinge.EN_SECHAGE, EtatLinge.SECHE, EtatLinge.PLIE));
        circuit.removeAll(etatsDisponibles());
        return circuit;
    }

    private Set<EtatLinge> etatsDeLEtape(CodeEtape etape) {
        return switch (etape) {
            case COLLECTE -> Set.of(EtatLinge.COLLECTE);
            case RECEPTION -> Set.of(EtatLinge.RECEPTION);
            case TRI -> Set.of(EtatLinge.EN_TRI);
            case LAVAGE -> Set.of(EtatLinge.EN_LAVAGE, EtatLinge.LAVE);
            case SECHAGE -> Set.of(EtatLinge.EN_SECHAGE, EtatLinge.SECHE);
            case PLIAGE -> Set.of(EtatLinge.PLIE);
            case STOCKAGE -> Set.of(EtatLinge.STOCKE);
            case DISTRIBUTION -> Set.of(EtatLinge.LIVRE);
        };
    }

    private List<Map<String, Object>> buildParcours(List<Linge> linges) {
        Map<EtatLinge, Long> parEtat = linges.stream()
                .filter(l -> l.getEtat() != null)
                .collect(Collectors.groupingBy(Linge::getEtat, Collectors.counting()));

        List<Map<String, Object>> resultat = new ArrayList<>();
        long reporte = 0;

        for (ParcoursService.EtapeResume etape : parcoursService.etapes()) {
            long propres = 0;
            for (EtatLinge etat : etatsDeLEtape(etape.code())) {
                propres += parEtat.getOrDefault(etat, 0L);
            }

            if (!etape.actif()) {
                reporte += propres;
                continue;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code",     etape.code().name());
            m.put("nom",      etape.nom());
            m.put("ordre",    etape.ordre());
            m.put("nbLinges", propres + reporte);
            if (reporte > 0) {
                m.put("dontEtapesDesactivees", reporte);
            }
            reporte = 0;
            resultat.add(m);
        }
        return resultat;
    }

    private String inferEtape(String typeOp) {
        if (typeOp == null || typeOp.isBlank()) {
            return "";
        }
        try {
            TypeOperation operation = TypeOperation.valueOf(typeOp);
            CodeEtape etape = OperationsEtape.etape(operation);
            return etape != null ? parcoursService.nomEtape(etape) : OperationsEtape.libelle(operation);
        } catch (IllegalArgumentException e) {
            return typeOp;
        }
    }
}