package tn.softsys.buanderie.service.management;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.ChambreRepository;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class ChambreService {
    private final ChambreRepository chambreRepository;
    public List<Chambre> findAll() { return chambreRepository.findAll(); }
    public Chambre findById(Long id) { return chambreRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Chambre", id)); }
   // public Chambre findByNumero(String numero) { return chambreRepository.findByNumero(numero).orElseThrow(() -> new ResourceNotFoundException("Chambre", "numero", numero)); }
   /* public Chambre creer(Chambre chambre) {
        if (chambreRepository.existsByNumero(chambre.getNumero())) throw new BusinessException("Chambre " + chambre.getNumero() + " existe déjà");
        return chambreRepository.save(chambre);
    }
    public Chambre modifier(Long id, Chambre data) {
        Chambre c = findById(id); c.setType(data.getType()); c.setEtage(data.getEtage());
        return chambreRepository.save(c);
    }*/
    public void supprimer(Long id) { chambreRepository.deleteById(id); }
}
