package tn.softsys.buanderie.Sqlserver.sqlServerServices;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerSejourRepository;
import tn.softsys.buanderie.Sqlserver.dtos.SejourDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Sejour;


@Service("sqlServerSejourService")
@RequiredArgsConstructor
public class SqlServerSejourService {
    private final SqlServerSejourRepository sejourRepository;

    public Page<SejourDTO> getAllSejours(int page, int size) {

        return sejourRepository
                .findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    public SejourDTO getSejourById(Long id) {

        Sejour sejour = sejourRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Séjour introuvable"));

        return mapToDTO(sejour);
    }

    public Page<SejourDTO> getSejoursByEtat(
            Integer etat,
            int page,
            int size
    ) {

        return sejourRepository
                .findByEtatChambreActual(
                        etat,
                        PageRequest.of(page, size)
                )
                .map(this::mapToDTO);
    }

    private SejourDTO mapToDTO(Sejour sejour) {

        return SejourDTO.builder()
                .id(sejour.getId())
                .etatChambreActual(sejour.getEtatChambreActual())
                .litId(sejour.getLit() != null ? sejour.getLit().getId() : null)
                .patientId(sejour.getPatientId())
                .virus(sejour.getVirus())
                .dateExit(sejour.getDateExit())
                .serviceId(sejour.getService() != null ? sejour.getService().getId() : null)
                .build();
    }

}
