package tn.softsys.buanderie.Sqlserver.sqlServerServices;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixChambreService;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerChambreRepository;
import tn.softsys.buanderie.Sqlserver.dtos.ChambreDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Chambre;
import tn.softsys.buanderie.dto.ChambreDto;

@Service("sqlServerChambreService")
@RequiredArgsConstructor
public class SqlServerChambreService {

    private final SqlServerChambreRepository chambreRepository;
    private final CubixChambreService cubixChambreService;

    public Page<ChambreDTO> getAllChambres(int page, int size) {
        return chambreRepository
                .findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    public ChambreDTO getChambreById(Long id) {
        Chambre chambre = chambreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chambre SQL Server introuvable"));
        return mapToDTO(chambre);
    }

    public Page<ChambreDTO> getChambresByServiceId(Long serviceId, int page, int size) {
        return chambreRepository
                .findByServiceId(serviceId, PageRequest.of(page, size))
                .map(this::mapToDTO);
    }
    public ChambreDto getChambreByCodeBarre(String code){
        ChambreDto chambre =  cubixChambreService.getByCodeBarre(code);
               // .orElseThrow(() -> new RuntimeException("Chambre SQL Server introuvable"));
        //return mapToDTO(chambre);
        return chambre;
    }


    private ChambreDTO mapToDTO(Chambre chambre) {
        return ChambreDTO.builder()
                .id(chambre.getId())
                .code(chambre.getCode())
                .codeBarre(chambre.getCodeBarre())
                .serviceId(chambre.getService() != null ? chambre.getService().getId() : null)
                .build();
    }
}
