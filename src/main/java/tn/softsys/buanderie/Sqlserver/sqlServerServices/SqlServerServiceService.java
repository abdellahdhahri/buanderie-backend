package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.Sqlserver.dtos.ServiceDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Service;


@org.springframework.stereotype.Service("sqlServerServiceService")
@RequiredArgsConstructor
public class SqlServerServiceService {

    private final SqlServerServiceRepository serviceRepository;

    public Page<ServiceDTO> getAllServices(int page, int size) {
        return serviceRepository
                .findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    public ServiceDTO getServiceById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service SQL Server introuvable"));
        return mapToDTO(service);
    }


    public ServiceDTO getByReference(String ref){
        Service service = serviceRepository.findByReference(ref);
        return mapToDTO(service);
    }

    private ServiceDTO mapToDTO(Service service) {
        return ServiceDTO.builder()
                .id(service.getId())
                .code(service.getCode())
                .description(service.getDescription())
                .reference(service.getReference())
                .build();
    }
}
