package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerMachineRepository;
import tn.softsys.buanderie.Sqlserver.dtos.MachineDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Article;

@Service("sqlServerMachineService")
@RequiredArgsConstructor
public class SqlServerMachineService {

    private final SqlServerMachineRepository machineRepository;

    public Page<MachineDTO> getAllMachines(int page, int size, int id) {
        return machineRepository
                .findByArticleFamilleId(PageRequest.of(page, size),22)
                .map(this::mapToDTO);
    }

    public MachineDTO getMachineById(Long id) {
        Article article = machineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Machine SQL Server introuvable"));
        return mapToDTO(article);
    }


    private MachineDTO mapToDTO(Article article) {
        return MachineDTO.builder()
                .id(article.getId())
                .articleFamilleId(article.getArticleFamilleId())
               // .codeBarre(article.get())
                .articleFamilleId(article.getArticleFamilleId())
                .build();
    }
}
