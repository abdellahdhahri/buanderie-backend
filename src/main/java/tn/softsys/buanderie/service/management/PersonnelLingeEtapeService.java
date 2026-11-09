package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.repository.PersonnelRepository;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.entity.PersonnelLingeEtape;
import tn.softsys.buanderie.repository.PersonnelLingeEtapeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonnelLingeEtapeService {

    private final PersonnelLingeEtapeRepository personnelLingeEtapeRepository;
    private final PersonnelRepository personnelRepository;



    public List<Personnel> getPersonnelByLingeEtapeId(Long lingeEtapeId) {

        //  Récupérer les associations depuis PostgreSQL
        List<PersonnelLingeEtape> associations =
                personnelLingeEtapeRepository
                        .findByLingeEtapeId(lingeEtapeId);

        // Récupérer uniquement les IDs des personnels
        List<Long> personnelIds = associations.stream()
                .map(PersonnelLingeEtape::getPersonnelId)
                .distinct()
                .toList();

        //  Aucun personnel affecté à cette étape
        if (personnelIds.isEmpty()) {
            return List.of();
        }

        //  Récupérer les personnels depuis SQL Server
        return personnelRepository.findByIdIn(personnelIds);
    }


    public List<PersonnelLingeEtape> getAll() {
        return personnelLingeEtapeRepository.findAll();
    }



    public PersonnelLingeEtape getById(Long id) {

        return personnelLingeEtapeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Association PersonnelLingeEtape introuvable avec l'id : " + id
                        )
                );
    }



    @Transactional
    public PersonnelLingeEtape create(
            PersonnelLingeEtape personnelLingeEtape
    ) {

        if (personnelLingeEtape.getPersonnelId() == null) {
            throw new RuntimeException(
                    "Le personnelId est obligatoire"
            );
        }

        if (personnelLingeEtape.getLingeEtapeId() == null) {
            throw new RuntimeException(
                    "Le lingeEtapeId est obligatoire"
            );
        }

        boolean exists =
                personnelLingeEtapeRepository
                        .existsByPersonnelIdAndLingeEtapeId(
                                personnelLingeEtape.getPersonnelId(),
                                personnelLingeEtape.getLingeEtapeId()
                        );

        if (exists) {
            throw new RuntimeException(
                    "Ce personnel est déjà affecté à cette étape"
            );
        }

        return personnelLingeEtapeRepository.save(
                personnelLingeEtape
        );
    }



    @Transactional
    public PersonnelLingeEtape update(
            Long id,
            PersonnelLingeEtape data
    ) {

        PersonnelLingeEtape existing = getById(id);

        if (data.getPersonnelId() == null) {
            throw new RuntimeException(
                    "Le personnelId est obligatoire"
            );
        }

        if (data.getLingeEtapeId() == null) {
            throw new RuntimeException(
                    "Le lingeEtapeId est obligatoire"
            );
        }

        boolean exists =
                personnelLingeEtapeRepository
                        .existsByPersonnelIdAndLingeEtapeId(
                                data.getPersonnelId(),
                                data.getLingeEtapeId()
                        );

        // Si l'association trouvée est différente de celle
        // que nous sommes en train de modifier
        if (exists &&
                (!existing.getPersonnelId()
                        .equals(data.getPersonnelId())
                        ||
                        !existing.getLingeEtapeId()
                                .equals(data.getLingeEtapeId()))) {

            throw new RuntimeException(
                    "Cette association existe déjà"
            );
        }

        existing.setPersonnelId(data.getPersonnelId());
        existing.setLingeEtapeId(data.getLingeEtapeId());

        return personnelLingeEtapeRepository.save(existing);
    }



    @Transactional
    public void delete(Long id) {

        if (!personnelLingeEtapeRepository.existsById(id)) {
            throw new RuntimeException(
                    "Association introuvable avec l'id : " + id
            );
        }

        personnelLingeEtapeRepository.deleteById(id);
    }


    public boolean exists(
            Long personnelId,
            Long lingeEtapeId
    ) {

        return personnelLingeEtapeRepository
                .existsByPersonnelIdAndLingeEtapeId(
                        personnelId,
                        lingeEtapeId
                );
    }
}