package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.Agent;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.AgentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    public List<Agent> findActifs() {
        return agentRepository.findByActifTrue();
    }

    public Agent findById(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
    }

    public Agent findByMatricule(String matricule) {
        return agentRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "matricule", matricule));
    }

    public Agent creer(Agent agent) {
        if (agentRepository.existsByMatricule(agent.getMatricule())) {
            throw new BusinessException("Un agent avec le matricule '" + agent.getMatricule() + "' existe déjà");
        }
        return agentRepository.save(agent);
    }

    public Agent modifier(Long id, Agent data) {
        Agent agent = findById(id);
        agent.setNom(data.getNom());
        agent.setPrenom(data.getPrenom());
        agent.setPoste(data.getPoste());
        agent.setTelephone(data.getTelephone());
        return agentRepository.save(agent);
    }

    public void desactiver(Long id) {
        Agent agent = findById(id);
        agent.setActif(false);
        agentRepository.save(agent);
    }
}
