package tn.softsys.buanderie.Sqlserver.sqlServerServices;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerChambreRepository;
import tn.softsys.buanderie.Sqlserver.dtos.ChambreDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Chambre;

@Service("sqlServerChambreService")
@RequiredArgsConstructor
public class SqlServerChambreService {

    private final SqlServerChambreRepository chambreRepository;

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
    public ChambreDTO getChambreByCodeBarre(String code){
        Chambre chambre =  chambreRepository.findByCodeBarre(code)
                .orElseThrow(() -> new RuntimeException("Chambre SQL Server introuvable"));
        return mapToDTO(chambre);
    }

    // on commence tt d'abord par les deux premier flux chambre +lit et services

    private ChambreDTO mapToDTO(Chambre chambre) {
        return ChambreDTO.builder()
                .id(chambre.getId())
                .code(chambre.getCode())
                .serviceId(chambre.getService() != null ? chambre.getService().getId() : null)
                .build();
    }
}
