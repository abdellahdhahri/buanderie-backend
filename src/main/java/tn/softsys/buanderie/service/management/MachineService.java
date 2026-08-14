package tn.softsys.buanderie.service.management;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.MachineRepository;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class MachineService {
    private final MachineRepository machineRepository;
    public List<Machine> findAll() { return machineRepository.findAll(); }
    public List<Machine> findDisponibles(String type) { return machineRepository.findByTypeAndEtat(type, Machine.EtatMachine.DISPONIBLE); }
    public Machine findById(Long id) { return machineRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Machine", id)); }
    public Machine findByCode(String code) { return machineRepository.findByCode(code); }

    public Machine creer(Machine machine) { return machineRepository.save(machine); }
    public Machine modifier(Long id, Machine data) {
        Machine m = findById(id); m.setMarque(data.getMarque()); m.setCapaciteKg(data.getCapaciteKg());
        m.setLocalisation(data.getLocalisation()); m.setEtat(data.getEtat()); return machineRepository.save(m);
    }
}
