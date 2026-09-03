package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.AutoclaveRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Autoclave;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoclaveService {

    private final AutoclaveRepository autoclaveRepository;

    public List<Autoclave> getLaveurs() {
        return autoclaveRepository.findByCategorie(2);
    }

    public List<Autoclave> getTous() {
        return autoclaveRepository.findAll();
    }
}