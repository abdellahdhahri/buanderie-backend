package tn.softsys.buanderie.service.management;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.OperationEtapeDTO;
import tn.softsys.buanderie.dto.ParcoursEtapeDTO;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.LingeEtapeSuivante;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.StatutCycle;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.CycleSechageRepository;
import tn.softsys.buanderie.repository.LingeEtapeRepository;
import tn.softsys.buanderie.repository.LingeEtapeSuivanteRepository;

@Service
@RequiredArgsConstructor
public class ParcoursService {

    private static final long DUREE_CACHE_MS = 10_000;

    private final LingeEtapeRepository etapeRepository;
    private final LingeEtapeSuivanteRepository suivanteRepository;
    private final CycleSechageRepository cycleSechageRepository;

    private volatile Graphe cache;
    private volatile long chargeLe;

    public boolean estActive(CodeEtape code) {
        return graphe().estActive(code);
    }

    public String nomEtape(CodeEtape code) {
        String nom = graphe().nom(code);
        return nom != null && !nom.isBlank() ? nom : code.getNom();
    }

    public List<EtapeResume> etapes() {
        Graphe graphe = graphe();
        List<EtapeResume> resultat = new ArrayList<>();
        for (CodeEtape code : CodeEtape.values()) {
            resultat.add(new EtapeResume(code, nomEtape(code), graphe.ordre(code), graphe.estActive(code)));
        }
        resultat.sort((a, b) -> Integer.compare(a.ordre(), b.ordre()));
        return resultat;
    }

    public void exigerActive(CodeEtape code) {
        if (!estActive(code)) {
            throw new BusinessException("L'étape « " + code.getNom() + " » est désactivée pour cet établissement.");
        }
    }

    public Set<EtatLinge> etatsAcceptes(CodeEtape code) {
        Graphe graphe = graphe();
        Set<EtatLinge> etats = new LinkedHashSet<>();
        collecterEtats(code, graphe, etats, new HashSet<>());
        return etats;
    }

    @Transactional(readOnly = true)
    public List<ParcoursEtapeDTO> parcours() {
        Graphe graphe = charger();
        List<ParcoursEtapeDTO> resultat = new ArrayList<>();

        for (LingeEtape etape : etapeRepository.findAllByOrderByOrdreAsc()) {
            CodeEtape code = etape.getCode();
            if (code == null) {
                continue;
            }
            Set<EtatLinge> etats = new LinkedHashSet<>();
            collecterEtats(code, graphe, etats, new HashSet<>());

            resultat.add(ParcoursEtapeDTO.builder()
                    .id(etape.getId())
                    .code(code)
                    .nom(etape.getNom())
                    .ordre(etape.getOrdre())
                    .actif(graphe.estActive(code))
                    .obligatoire(Boolean.TRUE.equals(etape.getObligatoire()))
                    .suivantes(new ArrayList<>(graphe.suivantes(code)))
                    .precedentes(new ArrayList<>(graphe.precedentes(code)))
                    .etatProduit(code.getEtatProduit())
                    .etatsAcceptes(new ArrayList<>(etats))
                    .build());
        }
        return resultat;
    }

    public List<OperationEtapeDTO> operations() {
        List<OperationEtapeDTO> resultat = new ArrayList<>();
        for (TypeOperation operation : TypeOperation.values()) {
            CodeEtape etape = OperationsEtape.etape(operation);
            resultat.add(OperationEtapeDTO.builder()
                    .typeOperation(operation)
                    .libelle(OperationsEtape.libelle(operation))
                    .etape(etape)
                    .nomEtape(etape != null ? nomEtape(etape) : null)
                    .build());
        }
        return resultat;
    }

    @Transactional
    public void definirActivation(Long etapeId, Boolean actif) {
        if (actif == null) {
            throw new BusinessException("L'état d'activation est obligatoire.");
        }
        LingeEtape etape = chargerEtape(etapeId);
        CodeEtape code = codeStandard(etape);

        if (actif.equals(!Boolean.FALSE.equals(etape.getActif()))) {
            return;
        }

        if (!actif) {
            if (Boolean.TRUE.equals(etape.getObligatoire())) {
                throw new BusinessException(
                        "L'étape « " + code.getNom() + " » est obligatoire : elle ne peut pas être désactivée.");
            }
            if (cycleEnCours(code)) {
                throw new BusinessException(
                        "Un cycle de « " + code.getNom() + " » est en cours : terminez-le avant de désactiver l'étape.");
            }
        }

        Graphe simule = charger();
        simule.definirActif(code, actif);
        verifier(simule);

        etape.setActif(actif);
        etapeRepository.save(etape);
        invalider();
    }

    @Transactional
    public void definirSuivantes(Long etapeId, List<String> codes) {
        LingeEtape etape = chargerEtape(etapeId);
        CodeEtape code = codeStandard(etape);

        Set<CodeEtape> souhaitees = EnumSet.noneOf(CodeEtape.class);
        if (codes != null) {
            for (String texte : codes) {
                if (texte == null || texte.isBlank()) {
                    continue;
                }
                CodeEtape cible;
                try {
                    cible = CodeEtape.valueOf(texte.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Code d'étape inconnu : " + texte.trim());
                }
                if (cible == code) {
                    throw new BusinessException("Une étape ne peut pas être sa propre suivante.");
                }
                souhaitees.add(cible);
            }
        }

        Graphe simule = charger();
        simule.definirSuivantes(code, souhaitees);
        verifier(simule);

        List<LingeEtapeSuivante> existantes = suivanteRepository.findByEtape_Id(etape.getId());
        Set<CodeEtape> presentes = EnumSet.noneOf(CodeEtape.class);
        List<LingeEtapeSuivante> aRetirer = new ArrayList<>();

        for (LingeEtapeSuivante lien : existantes) {
            CodeEtape cible = lien.getEtapeSuivante().getCode();
            if (cible != null && souhaitees.contains(cible)) {
                presentes.add(cible);
            } else {
                aRetirer.add(lien);
            }
        }
        suivanteRepository.deleteAll(aRetirer);

        for (CodeEtape cible : souhaitees) {
            if (presentes.contains(cible)) {
                continue;
            }
            LingeEtape etapeCible = etapeRepository.findByCode(cible)
                    .orElseThrow(() -> new BusinessException("Étape introuvable : " + cible.getNom()));
            LingeEtapeSuivante lien = new LingeEtapeSuivante();
            lien.setEtape(etape);
            lien.setEtapeSuivante(etapeCible);
            suivanteRepository.save(lien);
        }

        invalider();
    }

    public void invalider() {
        cache = null;
    }

    private Graphe graphe() {
        Graphe courant = cache;
        if (courant == null || System.currentTimeMillis() - chargeLe > DUREE_CACHE_MS) {
            courant = charger();
            cache = courant;
            chargeLe = System.currentTimeMillis();
        }
        return courant;
    }

    private Graphe charger() {
        Graphe graphe = new Graphe();

        for (LingeEtape etape : etapeRepository.findAll()) {
            if (etape.getCode() != null) {
                graphe.etapes.put(etape.getCode(), new EtatEtape(
                        !Boolean.FALSE.equals(etape.getActif()),
                        Boolean.TRUE.equals(etape.getObligatoire())));
                graphe.noms.put(etape.getCode(), etape.getNom());
                graphe.ordres.put(etape.getCode(), etape.getOrdre());
            }
        }

        for (LingeEtapeSuivante lien : suivanteRepository.findAllAvecEtapes()) {
            CodeEtape depart = lien.getEtape().getCode();
            CodeEtape arrivee = lien.getEtapeSuivante().getCode();
            if (depart != null && arrivee != null) {
                graphe.suivantes.computeIfAbsent(depart, k -> EnumSet.noneOf(CodeEtape.class)).add(arrivee);
            }
        }
        return graphe;
    }

    private void collecterEtats(CodeEtape cible, Graphe graphe, Set<EtatLinge> etats, Set<CodeEtape> vus) {
        for (CodeEtape precedente : graphe.precedentes(cible)) {
            if (!vus.add(precedente)) {
                continue;
            }
            etats.add(precedente.getEtatProduit());
            if (!graphe.estActive(precedente)) {
                collecterEtats(precedente, graphe, etats, vus);
            }
        }
    }

    private void verifier(Graphe graphe) {
        for (CodeEtape code : CodeEtape.values()) {
            if (!graphe.etapes.containsKey(code)) {
                throw new BusinessException("L'étape standard « " + code.getNom()
                        + " » est introuvable : redémarrez l'application pour la recréer.");
            }
        }

        for (Map.Entry<CodeEtape, EtatEtape> entree : graphe.etapes.entrySet()) {
            if (entree.getValue().obligatoire() && !entree.getValue().actif()) {
                throw new BusinessException("L'étape « " + entree.getKey().getNom() + " » est obligatoire.");
            }
        }

        Map<CodeEtape, Integer> visite = new EnumMap<>(CodeEtape.class);
        for (CodeEtape code : CodeEtape.values()) {
            verifierSansBoucle(code, graphe, visite);
        }

        for (CodeEtape code : CodeEtape.values()) {
            if (graphe.estActive(code) && !mene(code, CodeEtape.DISTRIBUTION, graphe, new HashSet<>())) {
                throw new BusinessException("Parcours incohérent : l'étape « " + code.getNom()
                        + " » ne mène plus à la distribution.");
            }
        }
    }

    private void verifierSansBoucle(CodeEtape code, Graphe graphe, Map<CodeEtape, Integer> visite) {
        Integer etat = visite.get(code);
        if (etat != null) {
            if (etat == 1) {
                throw new BusinessException("Parcours incohérent : boucle détectée autour de l'étape « "
                        + code.getNom() + " ».");
            }
            return;
        }
        visite.put(code, 1);
        for (CodeEtape suivante : graphe.suivantes(code)) {
            verifierSansBoucle(suivante, graphe, visite);
        }
        visite.put(code, 2);
    }

    private boolean mene(CodeEtape depart, CodeEtape cible, Graphe graphe, Set<CodeEtape> vus) {
        if (depart == cible) {
            return true;
        }
        if (!vus.add(depart)) {
            return false;
        }
        for (CodeEtape suivante : graphe.suivantes(depart)) {
            if (mene(suivante, cible, graphe, vus)) {
                return true;
            }
        }
        return false;
    }

    private boolean cycleEnCours(CodeEtape code) {
        return code == CodeEtape.SECHAGE
                && !cycleSechageRepository.findByStatut(StatutCycle.EN_COURS).isEmpty();
    }

    private LingeEtape chargerEtape(Long id) {
        return etapeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Étape introuvable : " + id));
    }

    private CodeEtape codeStandard(LingeEtape etape) {
        if (etape.getCode() == null) {
            throw new BusinessException("Cette étape n'est pas une étape standard du parcours.");
        }
        return etape.getCode();
    }

    public record EtapeResume(CodeEtape code, String nom, int ordre, boolean actif) {
    }

    private record EtatEtape(boolean actif, boolean obligatoire) {
    }

    private static final class Graphe {

        private final Map<CodeEtape, EtatEtape> etapes = new EnumMap<>(CodeEtape.class);
        private final Map<CodeEtape, Set<CodeEtape>> suivantes = new EnumMap<>(CodeEtape.class);
        private final Map<CodeEtape, String> noms = new EnumMap<>(CodeEtape.class);
        private final Map<CodeEtape, Integer> ordres = new EnumMap<>(CodeEtape.class);

        String nom(CodeEtape code) {
            return noms.get(code);
        }

        int ordre(CodeEtape code) {
            Integer ordre = ordres.get(code);
            return ordre != null ? ordre : code.getOrdreParDefaut();
        }

        boolean estActive(CodeEtape code) {
            EtatEtape etat = etapes.get(code);
            return etat != null && etat.actif();
        }

        Set<CodeEtape> suivantes(CodeEtape code) {
            return suivantes.getOrDefault(code, Set.of());
        }

        Set<CodeEtape> precedentes(CodeEtape code) {
            Set<CodeEtape> resultat = EnumSet.noneOf(CodeEtape.class);
            for (Map.Entry<CodeEtape, Set<CodeEtape>> entree : suivantes.entrySet()) {
                if (entree.getValue().contains(code)) {
                    resultat.add(entree.getKey());
                }
            }
            return resultat;
        }

        void definirActif(CodeEtape code, boolean actif) {
            EtatEtape ancien = etapes.get(code);
            etapes.put(code, new EtatEtape(actif, ancien != null && ancien.obligatoire()));
        }

        void definirSuivantes(CodeEtape code, Set<CodeEtape> nouvelles) {
            Set<CodeEtape> copie = EnumSet.noneOf(CodeEtape.class);
            copie.addAll(nouvelles);
            suivantes.put(code, copie);
        }
    }
}
