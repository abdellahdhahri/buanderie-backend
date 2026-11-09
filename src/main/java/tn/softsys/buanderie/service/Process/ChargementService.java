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
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.Lot;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.CodeEtape;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.repository.LotRepository;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.service.management.ParcoursService;

@Service
@RequiredArgsConstructor
public class ChargementService {

    private static final double SEUIL_ECART_POURCENT = 20.0;

    private final LotRepository lotRepository;
    private final LingeRepository lingeRepository;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final PersonnelRepository personnelRepository;
    private final ParcoursService parcoursService;

    @Transactional
    public ChargementDTO scanner(ScanChargementRequest request) {
        exigerTriDesactive();

        Personnel agent = agent(request.getMatriculeAgent());
        Lot lot = lot(request.getCodeLot());
        exigerLotOuvert(lot);

        Linge linge = linge(request.getCodeOuTag());
        List<String> messages = new ArrayList<>();

        if (linge.getLot() != null) {
            if (linge.getLot().getId().equals(lot.getId())) {
                messages.add("Linge déjà présent dans ce lot : ignoré.");
                return versDto(lot, messages);
            }
            throw new BusinessException("Le linge est déjà dans le lot « " + linge.getLot().getCodeLot() + " ».");
        }

        if (lot.getTypeCollecte() != linge.getTypeCollecte()) {
            throw new BusinessException("Le type de collecte du linge ne correspond pas à celui du lot.");
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

        linge.setLot(lot);
        linge.setEtat(EtatLinge.EN_TRI);
        linge.setBac(null);
        lingeRepository.save(linge);

        double poids = linge.getPoids() == null ? 0 : linge.getPoids();
        lot.setPoidsActuel(poidsActuel(lot) + poids);
        lotRepository.save(lot);

        historiser(linge, etatAvant, EtatLinge.EN_TRI, agent.getId(),
                "Chargement du lot " + lot.getCodeLot());

        return versDto(lot, messages);
    }

    @Transactional
    public ChargementDTO peser(String codeLot, PeseeChargementRequest request) {
        if (request.getPoids() == null || request.getPoids() <= 0) {
            throw new BusinessException("Le poids de la balance doit être supérieur à 0.");
        }

        Lot lot = lot(codeLot);
        if (lot.getEtat() != EtatLot.EN_TRI && lot.getEtat() != EtatLot.PRET_LAVAGE) {
            throw new BusinessException("Le lot « " + lot.getCodeLot()
                    + " » ne peut plus être pesé (état : " + lot.getEtat() + ").");
        }

        lot.setPoidsBalance(request.getPoids());
        lotRepository.save(lot);

        return versDto(lot, new ArrayList<>());
    }

    @Transactional
    public ChargementDTO valider(String codeLot, ValidationChargementRequest request) {
        Personnel agent = agent(request.getMatriculeAgent());
        Lot lot = lot(codeLot);
        exigerLotOuvert(lot);

        List<Linge> linges = lingeRepository.findByLot_CodeLot(lot.getCodeLot());
        if (linges.isEmpty()) {
            throw new BusinessException("Le lot « " + lot.getCodeLot() + " » ne contient aucun linge.");
        }

        if (request.getPoids() != null) {
            if (request.getPoids() <= 0) {
                throw new BusinessException("Le poids de la balance doit être supérieur à 0.");
            }
            lot.setPoidsBalance(request.getPoids());
        }
        if (lot.getPoidsBalance() == null) {
            throw new BusinessException("Saisissez le poids de la balance avant de valider le lot.");
        }

        if (!parcoursService.estActive(CodeEtape.TRI)) {
            CategLinge dominante = categorieDominante(linges);
            if (dominante != null) {
                lot.setCategorie(dominante);
            }
        }

        lot.setEtat(EtatLot.PRET_LAVAGE);
        lotRepository.save(lot);

        for (Linge linge : linges) {
            historiser(linge, EtatLinge.EN_TRI, EtatLinge.EN_TRI, agent.getId(),
                    "Chargement validé : lot " + lot.getCodeLot() + ", " + lot.getPoidsBalance() + " kg");
        }

        List<String> messages = new ArrayList<>();
        messages.add("Lot prêt pour le lavage.");
        return versDto(lot, messages);
    }

    @Transactional
    public ChargementDTO retirer(String codeLot, String codeOuTag, String matriculeAgent) {
        exigerTriDesactive();

        Personnel agent = agent(matriculeAgent);
        Lot lot = lot(codeLot);
        exigerLotOuvert(lot);

        Linge linge = linge(codeOuTag);
        if (linge.getLot() == null || !linge.getLot().getId().equals(lot.getId())) {
            throw new BusinessException("Ce linge n'est pas dans le lot « " + lot.getCodeLot() + " ».");
        }

        EtatLinge etatPrecedent = EtatLinge.RECEPTION;
        for (HistoriqueLinge h : historiqueLingeRepository.findByLingeIdOrderByDateOperationDesc(linge.getId())) {
            if (h.getEtatApres() == EtatLinge.EN_TRI && h.getEtatAvant() != null && h.getEtatAvant() != EtatLinge.EN_TRI) {
                etatPrecedent = h.getEtatAvant();
                break;
            }
        }

        linge.setLot(null);
        linge.setEtat(etatPrecedent);
        lingeRepository.save(linge);

        double poids = linge.getPoids() == null ? 0 : linge.getPoids();
        lot.setPoidsActuel(Math.max(0, poidsActuel(lot) - poids));
        lotRepository.save(lot);

        historiser(linge, EtatLinge.EN_TRI, etatPrecedent, agent.getId(),
                "Retiré du lot " + lot.getCodeLot());

        return versDto(lot, new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public ChargementDTO consulter(String codeLot) {
        return versDto(lot(codeLot), new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public List<ChargementDTO> enAttenteDeLavage() {
        List<ChargementDTO> resultat = new ArrayList<>();
        for (Lot lot : lotRepository.findByEtat(EtatLot.PRET_LAVAGE)) {
            resultat.add(versDto(lot, new ArrayList<>()));
        }
        return resultat;
    }

    private void exigerTriDesactive() {
        if (parcoursService.estActive(CodeEtape.TRI)) {
            throw new BusinessException(
                    "Le tri est actif : le chargement par pesée n'est disponible que lorsque l'étape Tri est désactivée.");
        }
    }

    private void exigerLotOuvert(Lot lot) {
        if (lot.getEtat() != EtatLot.EN_TRI) {
            throw new BusinessException("Le lot « " + lot.getCodeLot()
                    + " » n'est pas ouvert au chargement (état : " + lot.getEtat() + ").");
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

    private Lot lot(String codeLot) {
        if (codeLot == null || codeLot.isBlank()) {
            throw new BusinessException("Le code du lot est obligatoire.");
        }
        return lotRepository.findByCodeLot(codeLot.trim())
                .orElseThrow(() -> new BusinessException("Lot introuvable : " + codeLot));
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

    private double poidsActuel(Lot lot) {
        return lot.getPoidsActuel() == null ? 0 : lot.getPoidsActuel();
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

    private ChargementDTO versDto(Lot lot, List<String> messages) {
        List<Linge> linges = lingeRepository.findByLot_CodeLot(lot.getCodeLot());

        double declare = 0;
        List<String> codes = new ArrayList<>();
        for (Linge linge : linges) {
            declare += linge.getPoids() == null ? 0 : linge.getPoids();
            codes.add(linge.getCodeBarre());
        }

        Double ecart = null;
        Double balance = lot.getPoidsBalance();
        if (balance != null && declare > 0) {
            ecart = Math.abs(balance - declare) / balance * 100.0;
            if (ecart > SEUIL_ECART_POURCENT) {
                messages.add(String.format("Écart de %.0f %% entre la balance (%.1f kg) et la somme des poids déclarés (%.1f kg).",
                        ecart, balance, declare));
            }
        }

        return ChargementDTO.builder()
                .codeLot(lot.getCodeLot())
                .etatLot(lot.getEtat())
                .categorie(lot.getCategorie())
                .typeCollecte(lot.getTypeCollecte())
                .nbLinges(linges.size())
                .poidsDeclare(Math.round(declare * 10.0) / 10.0)
                .poidsBalance(balance)
                .ecartPourcent(ecart == null ? null : Math.round(ecart * 10.0) / 10.0)
                .pretPourLavage(lot.getEtat() == EtatLot.PRET_LAVAGE)
                .codesLinges(codes)
                .messages(messages)
                .build();
    }
}
