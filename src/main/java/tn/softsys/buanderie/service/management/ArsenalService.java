package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.repository.ArsenalRepository;
import tn.softsys.buanderie.repository.LingeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArsenalService {

    private final ArsenalRepository arsenalRepository;
    private final LingeRepository lingeRepository;


    public List<Arsenal> findAll() {
        return arsenalRepository.findAll();
    }


    public Arsenal findById(Long id) {
        return arsenalRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Arsenal introuvable avec l'id : " + id)
                );
    }


    public Arsenal save(Arsenal arsenal) {
        return arsenalRepository.save(arsenal);
    }



    public List<Linge> getLingesByArsenalId(Long arsenalId) {

        if (!arsenalRepository.existsById(arsenalId)) {
            throw new RuntimeException(
                    "Arsenal introuvable avec l'id : " + arsenalId
            );
        }

        return lingeRepository.findByEmplacement_Id(arsenalId);
    }

    public Arsenal update(Long id, Arsenal arsenalDetails) {

        Arsenal arsenal = arsenalRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Arsenal introuvable avec l'id : " + id)
                );

        arsenal.setNom(arsenalDetails.getNom());
        arsenal.setServiceId(arsenalDetails.getServiceId());



        return arsenalRepository.save(arsenal);
    }
}