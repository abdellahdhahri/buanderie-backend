package tn.softsys.buanderie.Sqlserver.sqlServerServices;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerArsenalRepository;
import tn.softsys.buanderie.Sqlserver.dtos.ArsenalDto;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Arsenal;


import java.util.List;

@Service
@RequiredArgsConstructor
public class SqlServerArsenalService {

    private final SqlServerArsenalRepository arsenalRepository;


    public List<ArsenalDto> getAll() {

        return arsenalRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    public ArsenalDto getArsenalById(Long id) {
        Arsenal arsenal = arsenalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("arsenal SQL Server introuvable"));
        return mapToDTO(arsenal);
    }


    private ArsenalDto mapToDTO(Arsenal arsenal) {

        return ArsenalDto.builder()
                .id(arsenal.getId())
                .categorie(arsenal.getCategorie())
                .LotArticle_ImmobilistationID(
                        arsenal.getArticle() != null
                                ? arsenal.getArticle().getId()
                                : null
                )
                .build();
    }
}