package tn.softsys.buanderie.service.management;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.dto.Referentiel.PersonnelRequest;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.service.referentiel.IntegrationService;

@Service
@RequiredArgsConstructor
public class PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final IntegrationService integrationService;
    private final tn.softsys.buanderie.repository.ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<Personnel> getAll(boolean actifsSeulement) {
        return actifsSeulement
                ? personnelRepository.findByActifTrueOrderByNomAscPrenomAsc()
                : personnelRepository.findAllByOrderByNomAscPrenomAsc();
    }

    @Transactional(readOnly = true)
    public Personnel getById(Long id) {
        return personnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel", id));
    }

    @Transactional(readOnly = true)
    public Personnel getByCodeBarre(String codeBarre) {
        Personnel personnel = personnelRepository.findByCodeBarre(codeBarre);
        if (personnel == null) {
            throw new BusinessException("Personnel introuvable : " + codeBarre);
        }
        return personnel;
    }

    @Transactional
    public Personnel creer(PersonnelRequest request) {
        if (integrationService.estImporte(Referentiel.PERSONNEL)) {
            throw new BusinessException(
                    "Le personnel est importé depuis le système externe : ajoutez-le dans le système source.");
        }

        String code = request.getCodeBarre().trim();
        if (personnelRepository.existsByCodeBarreAndActifTrue(code)) {
            throw new BusinessException("Un personnel actif utilise déjà le badge " + code + ".");
        }

        Personnel personnel = Personnel.builder()
                .source(SourceDonnee.INTERNE)
                .build();
        appliquer(personnel, request);
        return personnelRepository.save(personnel);
    }

    @Transactional
    public Personnel modifier(Long id, PersonnelRequest request) {
        Personnel personnel = getById(id);
        verifierModifiable(personnel);

        String code = request.getCodeBarre().trim();
        if (personnelRepository.existsByCodeBarreAndActifTrueAndIdNot(code, id)) {
            throw new BusinessException("Un personnel actif utilise déjà le badge " + code + ".");
        }

        appliquer(personnel, request);
        return personnelRepository.save(personnel);
    }

    @Transactional
    public Personnel changerEtat(Long id, boolean actif) {
        Personnel personnel = getById(id);
        verifierModifiable(personnel);

        if (actif && personnel.getCodeBarre() != null
                && personnelRepository.existsByCodeBarreAndActifTrueAndIdNot(personnel.getCodeBarre(), id)) {
            throw new BusinessException("Un personnel actif utilise déjà le badge " + personnel.getCodeBarre() + ".");
        }

        personnel.setActif(actif);
        return personnelRepository.save(personnel);
    }

    @Transactional
    public Personnel definirServiceAffectation(Long id, Long serviceAffectationId) {
        Personnel personnel = getById(id);
        if (serviceAffectationId != null && !serviceRepository.existsById(serviceAffectationId)) {
            throw new ResourceNotFoundException("Service", serviceAffectationId);
        }
        personnel.setServiceAffectationId(serviceAffectationId);
        return personnelRepository.save(personnel);
    }

    public PersonnelDto versDto(Personnel p) {
        return PersonnelDto.builder()
                .id(p.getId())
                .nom(p.getNom())
                .prenom(p.getPrenom())
                .nomPrenom(p.getNomPrenom())
                .matricule(p.getMatricule())
                .codeBarre(p.getCodeBarre())
                .fonction(p.getFonction())
                .fonctionId(p.getFonctionId())
                .serviceId(p.getServiceId())
                .serviceAffectationId(p.getServiceAffectationId())
                .visible(p.getActif())
                .estMasquer(!Boolean.TRUE.equals(p.getActif()))
                .build();
    }

    private void verifierModifiable(Personnel personnel) {
        if (personnel.getSource() != SourceDonnee.INTERNE) {
            throw new BusinessException(
                    "Ce personnel est synchronisé depuis " + personnel.getSource()
                            + " : modifiez-le dans le système source.");
        }
    }

    private void appliquer(Personnel p, PersonnelRequest r) {
        p.setNom(nettoyer(r.getNom()));
        p.setPrenom(nettoyer(r.getPrenom()));
        p.setMatricule(nettoyer(r.getMatricule()));
        p.setCodeBarre(nettoyer(r.getCodeBarre()));
        p.setFonction(nettoyer(r.getFonction()));
        p.setFonctionId(r.getFonctionId());
        p.setServiceId(r.getServiceId());
        if (r.getActif() != null) {
            p.setActif(r.getActif());
        }
    }

    private String nettoyer(String valeur) {
        if (valeur == null) {
            return null;
        }
        String v = valeur.trim();
        return v.isEmpty() ? null : v;
    }
}
