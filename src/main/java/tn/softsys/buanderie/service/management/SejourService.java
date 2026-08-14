package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.Sejour;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.SejourRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SejourService {

    private final SejourRepository sejourRepository;
    private final ChambreRepository chambreRepository;

    public List<Sejour> findAll() {
        return sejourRepository.findAll();
    }

    public List<Sejour> findByChambre(Long chambreId) {
        return sejourRepository.findByChambreId(chambreId);
    }

    public Sejour findById(Long id) {
        return sejourRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sejour", id));
    }

    public Sejour creer(Sejour sejour, Long chambreId) {
        Chambre chambre = chambreRepository.findById(chambreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre", chambreId));
        sejour.setChambre(chambre);
        return sejourRepository.save(sejour);
    }

    public void supprimer(Long id) {
        sejourRepository.deleteById(id);
    }
}
