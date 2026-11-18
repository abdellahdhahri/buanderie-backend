package tn.softsys.buanderie.service.management;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.RemiseHistoriqueDTO;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.RemiseLinge;
import tn.softsys.buanderie.entity.RemiseLingeItem;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.EtatLivraison;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.repository.RemiseLingeRepository;
import tn.softsys.buanderie.repository.ServiceRepository;

@Service
@RequiredArgsConstructor
public class RemiseHistoriqueService {

    private final RemiseLingeRepository remiseRepository;
    private final ServiceRepository serviceRepository;
    private final PersonnelRepository personnelRepository;

    @Transactional(readOnly = true)
    public Page<RemiseHistoriqueDTO> rechercher(Long serviceId, Long personnelId, EtatLivraison etat,
                                                LocalDate dateDebut, LocalDate dateFin, String numero,
                                                int page, int size) {
        Specification<RemiseLinge> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (serviceId != null) {
                p.add(cb.equal(root.get("service"), serviceId));
            }
            if (personnelId != null) {
                p.add(cb.equal(root.get("personnelId"), personnelId));
            }
            if (etat != null) {
                p.add(cb.equal(root.get("etat"), etat));
            }
            if (dateDebut != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("dateLivraison"), dateDebut.atStartOfDay()));
            }
            if (dateFin != null) {
                p.add(cb.lessThan(root.get("dateLivraison"), dateFin.plusDays(1).atStartOfDay()));
            }
            if (numero != null && !numero.isBlank()) {
                p.add(cb.like(cb.lower(root.get("numeroLivraison")), "%" + numero.trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        Page<RemiseLinge> resultat = remiseRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                        Sort.by(Sort.Direction.DESC, "dateLivraison")));

        Set<Long> serviceIds = resultat.stream().map(RemiseLinge::getService).collect(Collectors.toSet());
        Set<Long> personnelIds = resultat.stream().map(RemiseLinge::getPersonnelId).collect(Collectors.toSet());
        Map<Long, String> services = new HashMap<>();
        serviceRepository.findAllById(serviceIds).forEach(s -> services.put(s.getId(), s.getNom()));
        Map<Long, String> personnels = new HashMap<>();
        personnelRepository.findAllById(personnelIds).forEach(pe -> personnels.put(pe.getId(), nomComplet(pe)));

        return resultat.map(r -> versDto(r, services, personnels, false));
    }

    @Transactional(readOnly = true)
    public RemiseHistoriqueDTO detail(Long id) {
        RemiseLinge r = remiseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remise", id));
        Map<Long, String> services = new HashMap<>();
        serviceRepository.findById(r.getService()).ifPresent((ServiceEntity s) -> services.put(s.getId(), s.getNom()));
        Map<Long, String> personnels = new HashMap<>();
        personnelRepository.findById(r.getPersonnelId()).ifPresent(pe -> personnels.put(pe.getId(), nomComplet(pe)));
        return versDto(r, services, personnels, true);
    }

    private RemiseHistoriqueDTO versDto(RemiseLinge r, Map<Long, String> services, Map<Long, String> personnels,
                                        boolean avecDetail) {
        int collecte = 0;
        int remis = 0;
        int restant = 0;
        List<RemiseHistoriqueDTO.Ligne> lignes = new ArrayList<>();
        for (RemiseLingeItem it : r.getItems()) {
            int c = it.getQuantiteCollectee() == null ? 0 : it.getQuantiteCollectee();
            int rm = it.getQuantiteRemise() == null ? 0 : it.getQuantiteRemise();
            int reste = it.getQuantiteRestante() == null ? 0 : it.getQuantiteRestante();
            collecte += c;
            remis += rm;
            restant += reste;
            lignes.add(RemiseHistoriqueDTO.Ligne.builder()
                    .categorie(it.getCategorie() == null ? null : it.getCategorie().name())
                    .quantiteCollectee(c).quantiteRemise(rm).quantiteRestante(reste).build());
        }
        return RemiseHistoriqueDTO.builder()
                .id(r.getId())
                .numeroLivraison(r.getNumeroLivraison())
                .dateLivraison(r.getDateLivraison())
                .etat(r.getEtat() == null ? null : r.getEtat().name())
                .cloturee(r.getEtat() == EtatLivraison.COMPLETEE)
                .serviceId(r.getService())
                .serviceNom(services.get(r.getService()))
                .personnelId(r.getPersonnelId())
                .personnelNom(personnels.get(r.getPersonnelId()))
                .collecteId(r.getCollecte() == null ? null : r.getCollecte().getId())
                .numeroCollecte(r.getCollecte() == null ? null : r.getCollecte().getNumeroCollecte())
                .totalCollecte(collecte)
                .totalRemis(remis)
                .totalRestant(restant)
                .items(avecDetail ? lignes : null)
                .build();
    }

    private String nomComplet(Personnel p) {
        String prenom = p.getPrenom() == null ? "" : p.getPrenom();
        String nom = p.getNom() == null ? "" : p.getNom();
        return (prenom + " " + nom).trim();
    }
}
