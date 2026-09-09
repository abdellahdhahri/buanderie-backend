package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.enums.Zone;
import tn.softsys.buanderie.repository.LingeEtapeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LingeEtapeService {

    private final LingeEtapeRepository repository;

    public List<LingeEtape> getAll() {
        return repository.findAllByOrderByOrdreAsc();
    }

    public LingeEtape getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("LingeEtape introuvable avec l'id : " + id));
    }

    public LingeEtape getByCodeBarre(String codeBarre) {
        return repository.findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new RuntimeException(
                                "LingeEtape introuvable avec le code barre : " + codeBarre
                        ));
    }

    public List<LingeEtape> getByZone(Zone zone) {
        return repository.findByZone(zone);
    }

    public List<LingeEtape> getByCategorie(String categorie) {
        return repository.findByCategorie(categorie);
    }

    public List<LingeEtape> getByZoneAndCategorie(
            String zone,
            String categorie
    ) {
        return repository.findByZoneAndCategorie(zone, categorie);
    }

    @Transactional
    public LingeEtape create(LingeEtape lingeEtape) {

        // Récupérer le dernier ordre
        Integer dernierOrdre = repository.findMaxOrdre();

        int nouvelOrdre = (dernierOrdre == null)
                ? 1
                : dernierOrdre + 1;

        String codeBarre = String.format("E%04d", nouvelOrdre);

        lingeEtape.setOrdre(nouvelOrdre);
        lingeEtape.setCodeBarre(codeBarre);

        return repository.save(lingeEtape);
    }

    public LingeEtape update(Long id, LingeEtape data) {

        LingeEtape existing = getById(id);

        if (!existing.getCodeBarre().equals(data.getCodeBarre())
                && repository.existsByCodeBarre(data.getCodeBarre())) {

            throw new RuntimeException(
                    "Le code barre existe déjà : "
                            + data.getCodeBarre()
            );
        }

        existing.setNom(data.getNom());
        existing.setCodeBarre(data.getCodeBarre());
        existing.setCategorie(data.getCategorie());
        existing.setZone(data.getZone());

        return repository.save(existing);
    }

    public void delete(Long id) {
        LingeEtape existing = getById(id);
        repository.delete(existing);
    }
}