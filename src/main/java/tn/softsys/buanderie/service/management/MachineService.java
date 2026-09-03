package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.entity.enums.EtatTechniqueMachine;
import tn.softsys.buanderie.entity.enums.TypeMachine;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.MachineRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MachineService {

    private final MachineRepository machineRepository;

    public List<Machine> findAll() {
        return machineRepository.findAll();
    }

    public List<Machine> findDisponibles(String type) {

        TypeMachine typeMachine;

        if (type != null && !type.isBlank()) {
            typeMachine = TypeMachine.valueOf(type.toUpperCase());
        } else {
            typeMachine = TypeMachine.LAVEUR;
        }

        return machineRepository.findByTypeAndEtatTechnique(
                typeMachine,
                EtatTechniqueMachine.FONCTIONNEL
        );
    }

    public Machine findById(Long id) {

        return machineRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Machine", id)
                );
    }

    public Machine findByCode(String code) {

        Machine machine = machineRepository.findByCode(code);

        if (machine == null) {
            throw new ResourceNotFoundException(
                    "Machine avec le code " + code + " introuvable"
            );
        }

        return machine;
    }

    public Machine creer(Machine machine) {

        return machineRepository.save(machine);
    }

    public Machine modifier(Long id, Machine data) {

        Machine m = findById(id);

        m.setMarque(data.getMarque());
        m.setCapaciteKg(data.getCapaciteKg());
        m.setLocalisation(data.getLocalisation());
        m.setEtatImmobilisation(data.getEtatImmobilisation());

        return machineRepository.save(m);
    }

    public void supprimer(Long id) {

        Machine machine = findById(id);

        machineRepository.delete(machine);
    }
}