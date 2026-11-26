package tn.softsys.buanderie.service.Process;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ChargementDTO;
import tn.softsys.buanderie.dto.PeseeChargementRequest;
import tn.softsys.buanderie.dto.ScanChargementRequest;
import tn.softsys.buanderie.dto.ValidationChargementRequest;
import tn.softsys.buanderie.entity.Bac;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.GroupeLavage;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.entity.enums.UsageBac;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.BacRepository;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.service.management.ParcoursService;

@Service
@RequiredArgsConstructor
public class ChargementService {

    private static final double SEUIL_ECART_POURCENT = 20.0;

    private final BacRepository bacRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final PersonnelRepository personnelRepository;
    private final ParcoursService parcoursService;

    @Transactional
    public ChargementDTO scanner(ScanChargementRequest request) {
        exigerTriDesactive();

        Personnel agent = agent(request.getMatriculeAgent());
        Bac bac = bac(request.getCodeBac());
        exigerBacDeTri(bac);
        exigerBacOuvert(bac);

        Linge linge = linge(request.getCodeOuTag());
        List<String> messages = new ArrayList<>();

        if (linge.getBac() != null) {
            if (linge.getBac().getId().equals(bac.getId())) {
                messages.add("Linge déjà présent dans ce bac : ignoré.");
                return versDto(bac, messages);
            }
            throw new BusinessException("Le linge est déjà dans le bac « " + linge.getBac().getCode() + " ».");
        }

        if (bac.getTypeCollecte() != linge.getTypeCollecte()) {
            throw new BusinessException("Le type de collecte du linge ne correspond pas à celui du bac.");
        }

        Set<EtatLinge> etatsAcceptes = new LinkedHashSet<>(parcoursService.etatsAcceptes(CodeEtape.LAVAGE));
        if (etatsAcceptes.isEmpty()) {
            etatsAcceptes.add(EtatLinge.RECEPTION);
        }
        if (!etatsAcceptes.contains(linge.getEtat())) {
            throw new BusinessException("Le linge doit être dans l'un des états suivants : "
                    + String.join(", ", etatsAcceptes.stream().map(Enum::name).toList())
                    + " (état actuel : " + linge.getEtat() + ").");
        }

        EtatLinge etatAvant = linge.getEtat();

        linge.setBac(bac);
        linge.setEtat(EtatLinge.EN_TRI);
        lingeRepository.save(linge);

        double poids = linge.getPoids() == null ? 0 : linge.getPoids();
        bac.setPoids(poidsDeclare(bac) + poids);
        bac.setEtat(EtatBac.EN_TRI);
        bacRepository.save(bac);

        historiser(linge, etatAvant, EtatLinge.EN_TRI, agent.getId(),
                "Chargement du bac " + bac.getCode());

        return versDto(bac, messages);
    }

    @Transactional
    public ChargementDTO peser(String codeBac, PeseeChargementRequest request) {
        if (request.getPoids() == null || request.getPoids() <= 0) {
            throw new BusinessException("Le poids de la balance doit être supérieur à 0.");
        }

        Bac bac = bac(codeBac);
        exigerBacDeTri(bac);
        if (bac.getEtat() != EtatBac.EN_TRI && bac.getEtat() != EtatBac.PRET_LAVAGE) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » ne peut plus être pesé (état : " + bac.getEtat() + ").");
        }

        bac.setPoidsBalance(request.getPoids());
        bacRepository.save(bac);

        return versDto(bac, new ArrayList<>());
    }

    @Transactional
    public ChargementDTO valider(String codeBac, ValidationChargementRequest request) {
        Personnel agent = agent(request.getMatriculeAgent());
        Bac bac = bac(codeBac);
        exigerBacDeTri(bac);
        exigerBacOuvert(bac);

        List<Linge> linges = lingeRepository.findByBacId(bac.getId());
        if (linges.isEmpty()) {
            throw new BusinessException("Le bac « " + bac.getCode() + " » ne contient aucun linge.");
        }

        if (request.getPoids() != null) {
            if (request.getPoids() <= 0) {
                throw new BusinessException("Le poids de la balance doit être supérieur à 0.");
            }
            bac.setPoidsBalance(request.getPoids());
        }
        if (bac.getPoidsBalance() == null) {
            throw new BusinessException("Saisissez le poids de la balance avant de valider le bac.");
        }

        if (!parcoursService.estActive(CodeEtape.TRI)) {
            CategLinge dominante = categorieDominante(linges);
            if (dominante != null) {
                bac.setCategorie(dominante);
            }
        }

        bac.setEtat(EtatBac.PRET_LAVAGE);
        bacRepository.save(bac);

        for (Linge linge : linges) {
            historiser(linge, EtatLinge.EN_TRI, EtatLinge.EN_TRI, agent.getId(),
                    "Chargement validé : bac " + bac.getCode() + ", " + bac.getPoidsBalance() + " kg");
        }

        List<String> messages = new ArrayList<>();
        messages.add("Bac prêt pour le lavage.");
        return versDto(bac, messages);
    }

    @Transactional
    public ChargementDTO retirer(String codeBac, String codeOuTag, String matriculeAgent) {
        exigerTriDesactive();

        Personnel agent = agent(matriculeAgent);
        Bac bac = bac(codeBac);
        exigerBacDeTri(bac);
        exigerBacOuvert(bac);

        Linge linge = linge(codeOuTag);
        if (linge.getBac() == null || !linge.getBac().getId().equals(bac.getId())) {
            throw new BusinessException("Ce linge n'est pas dans le bac « " + bac.getCode() + " ».");
        }

        EtatLinge etatPrecedent = EtatLinge.RECEPTION;
        for (HistoriqueLinge h : historiqueLingeRepository.findByLingeIdOrderByDateOperationDesc(linge.getId())) {
            if (h.getEtatApres() == EtatLinge.EN_TRI && h.getEtatAvant() != null && h.getEtatAvant() != EtatLinge.EN_TRI) {
                etatPrecedent = h.getEtatAvant();
                break;
            }
        }

        linge.setBac(null);
        linge.setEtat(etatPrecedent);
        lingeRepository.save(linge);

        double poids = linge.getPoids() == null ? 0 : linge.getPoids();
        bac.setPoids(Math.max(0, poidsDeclare(bac) - poids));
        if (lingeRepository.findByBacId(bac.getId()).isEmpty()) {
            bac.setEtat(EtatBac.VIDE);
            bac.setPoids(0.0);
            bac.setPoidsBalance(null);
        }
        bacRepository.save(bac);

        historiser(linge, EtatLinge.EN_TRI, etatPrecedent, agent.getId(),
                "Retiré du bac " + bac.getCode());

        return versDto(bac, new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public ChargementDTO consulter(String codeBac) {
        Bac bac = bac(codeBac);
        exigerBacDeTri(bac);
        return versDto(bac, new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public List<ChargementDTO> enAttenteDeLavage() {
        List<ChargementDTO> resultat = new ArrayList<>();
        for (Bac bac : bacRepository.findByEtat(EtatBac.PRET_LAVAGE)) {
            if (bac.getUsage() == UsageBac.TRI) {
                resultat.add(versDto(bac, new ArrayList<>()));
            }
        }
        return resultat;
    }

    private void exigerTriDesactive() {
        if (parcoursService.estActive(CodeEtape.TRI)) {
            throw new BusinessException(
                    "Le tri est actif : le chargement par pesée n'est disponible que lorsque l'étape Tri est désactivée.");
        }
    }

    private void exigerBacDeTri(Bac bac) {
        if (bac.getUsage() != UsageBac.TRI) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas un bac de tri (usage : " + bac.getUsage() + ").");
        }
    }

    private void exigerBacOuvert(Bac bac) {
        if (bac.getEtat() != EtatBac.VIDE && bac.getEtat() != EtatBac.EN_TRI) {
            throw new BusinessException("Le bac « " + bac.getCode()
                    + " » n'est pas ouvert au chargement (état : " + bac.getEtat() + ").");
        }
    }

    private Personnel agent(String matricule) {
        Personnel agent = matricule == null || matricule.isBlank()
                ? null
                : personnelRepository.findByCodeBarre(matricule.trim());
        if (agent == null) {
            throw new BusinessException("Agent introuvable : " + matricule);
        }
        return agent;
    }

    private Bac bac(String codeBac) {
        if (codeBac == null || codeBac.isBlank()) {
            throw new BusinessException("Le code du bac est obligatoire.");
        }
        String code = codeBac.trim();
        return bacRepository.findByCode(code)
                .or(() -> bacRepository.findByRfidTag(code))
                .orElseThrow(() -> new BusinessException("Bac introuvable : " + code));
    }

    private Linge linge(String codeOuTag) {
        if (codeOuTag == null || codeOuTag.isBlank()) {
            throw new BusinessException("Le code-barres ou le tag du linge est obligatoire.");
        }
        String code = codeOuTag.trim();
        Linge linge = lingeRepository.findByCodeBarre(code).orElse(null);
        if (linge == null) {
            linge = lingeRepository.findByRfidTag(code);
        }
        if (linge == null) {
            throw new BusinessException("Linge introuvable : " + code);
        }
        return linge;
    }

    private double poidsDeclare(Bac bac) {
        return bac.getPoids() == null ? 0 : bac.getPoids();
    }

    private CategLinge categorieDominante(List<Linge> linges) {
        Map<CategLinge, Integer> compte = new EnumMap<>(CategLinge.class);
        for (Linge linge : linges) {
            if (linge.getCategorie() != null) {
                compte.merge(linge.getCategorie(), 1, Integer::sum);
            }
        }
        CategLinge dominante = null;
        int max = 0;
        for (Map.Entry<CategLinge, Integer> entree : compte.entrySet()) {
            if (entree.getValue() > max) {
                max = entree.getValue();
                dominante = entree.getKey();
            }
        }
        return dominante;
    }

    private String groupeDuBac(Bac bac, List<Linge> linges) {
        if (bac.getGroupeLavage() != null) return bac.getGroupeLavage().name();
        if (linges.isEmpty()) return null;
        long couleur = linges.stream()
                .filter(l -> (l.getGroupeLavage() != null ? l.getGroupeLavage()
                        : GroupeLavage.depuisDesignation(l.getDesignation())) == GroupeLavage.COULEUR)
                .count();
        return (couleur * 2 > linges.size() ? GroupeLavage.COULEUR : GroupeLavage.BLANC).name();
    }

    private void historiser(Linge linge, EtatLinge avant, EtatLinge apres, Long personnelId, String observation) {
        historiqueLingeRepository.save(HistoriqueLinge.builder()
                .linge(linge)
                .etatAvant(avant)
                .etatApres(apres)
                .personnelId(personnelId)
                .typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
                .dateOperation(LocalDateTime.now())
                .observations(observation)
                .build());
    }

    private ChargementDTO versDto(Bac bac, List<String> messages) {
        List<Linge> linges = lingeRepository.findByBacId(bac.getId());

        double declare = 0;
        List<String> codes = new ArrayList<>();
        for (Linge linge : linges) {
            declare += linge.getPoids() == null ? 0 : linge.getPoids();
            codes.add(linge.getCodeBarre());
        }

        // Les linges synchronisés n'ont souvent pas de poids : on retombe sur le poids du bac.
        if (declare <= 0 && bac.getPoids() != null) {
            declare = bac.getPoids();
        }

        Double ecart = null;
        Double balance = bac.getPoidsBalance();
        if (balance != null && declare > 0) {
            ecart = Math.abs(balance - declare) / balance * 100.0;
            if (ecart > SEUIL_ECART_POURCENT) {
                messages.add(String.format("Écart de %.0f %% entre la balance (%.1f kg) et la somme des poids déclarés (%.1f kg).",
                        ecart, balance, declare));
            }
        }

        return ChargementDTO.builder()
                .codeBac(bac.getCode())
                .etatBac(bac.getEtat())
                .categorie(bac.getCategorie())
                .typeCollecte(bac.getTypeCollecte())
                .groupeLavage(groupeDuBac(bac, linges))
                .nbLinges(linges.size())
                .poidsDeclare(Math.round(declare * 10.0) / 10.0)
                .poidsBalance(balance)
                .ecartPourcent(ecart == null ? null : Math.round(ecart * 10.0) / 10.0)
                .pretPourLavage(bac.getEtat() == EtatBac.PRET_LAVAGE)
                .codesLinges(codes)
                .messages(messages)
                .build();
    }
}
