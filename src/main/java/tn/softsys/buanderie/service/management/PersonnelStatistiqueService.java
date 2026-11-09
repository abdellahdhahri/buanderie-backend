package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.service.referentiel.PersonnelReferentielService;
import tn.softsys.buanderie.dto.PersonnelDto;
import tn.softsys.buanderie.dto.PersonnelStatDTO;
import tn.softsys.buanderie.entity.enums.TypeOperation;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonnelStatistiqueService {

    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final PersonnelReferentielService personnelService;

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        throw new IllegalStateException(
                "Type de date inattendu depuis la base : " + value.getClass()
        );
    }

    public List<PersonnelStatDTO> getStatistiquesPersonnel(
            LocalDateTime debut,
            LocalDateTime fin
    ) {

        // Totaux par personnel — on ignore les lignes sans personnel_id (données incohérentes)
        Map<Long, Long> totaux = historiqueLingeRepository
                .countTotalParPersonnel(debut, fin)
                .stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        // Répartition par type d'opération
        Map<Long, Map<TypeOperation, Long>> parType = new HashMap<>();
        for (Object[] row : historiqueLingeRepository.countParPersonnelEtType(debut, fin)) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            Long personnelId = ((Number) row[0]).longValue();
            TypeOperation type = TypeOperation.valueOf((String) row[1]);
            Long count = ((Number) row[2]).longValue();

            parType
                    .computeIfAbsent(personnelId, k -> new EnumMap<>(TypeOperation.class))
                    .put(type, count);
        }

        // Première / dernière opération -> pour la cadence moyenne
        Map<Long, LocalDateTime[]> periodes = new HashMap<>();
        for (Object[] row : historiqueLingeRepository.periodeActiviteParPersonnel(debut, fin)) {
            if (row[0] == null) {
                continue;
            }
            Long personnelId = ((Number) row[0]).longValue();
            LocalDateTime premiere = toLocalDateTime(row[1]);
            LocalDateTime derniere = toLocalDateTime(row[2]);
            periodes.put(personnelId, new LocalDateTime[]{premiere, derniere});
        }

        List<PersonnelStatDTO> resultat = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : totaux.entrySet()) {

            Long personnelId = entry.getKey();
            Long total = entry.getValue();

            String nomPrenom = "Inconnu";
            String matricule = "—";

            try {
                PersonnelDto personnel = personnelService.getById(personnelId);
                if (personnel != null) {
                    nomPrenom = (personnel.getNom() + " " + personnel.getPrenom()).trim();
                    matricule = personnel.getCodeBarre();
                }
            } catch (Exception e) {
                // Personnel introuvable côté SQL Server (supprimé, désactivé...) : on garde "Inconnu"
            }

            LocalDateTime[] periode = periodes.getOrDefault(
                    personnelId,
                    new LocalDateTime[]{null, null}
            );

            Double moyenneParJour = null;
            if (periode[0] != null && periode[1] != null) {
                long jours = ChronoUnit.DAYS.between(periode[0].toLocalDate(), periode[1].toLocalDate()) + 1;
                moyenneParJour = jours > 0
                        ? Math.round((total / (double) jours) * 100.0) / 100.0
                        : total.doubleValue();
            }

            resultat.add(PersonnelStatDTO.builder()
                    .personnelId(personnelId)
                    .nomPrenom(nomPrenom)
                    .matricule(matricule)
                    .totalOperations(total)
                    .operationsParType(parType.getOrDefault(personnelId, Collections.emptyMap()))
                    .premiereOperation(periode[0])
                    .derniereOperation(periode[1])
                    .moyenneParJour(moyenneParJour)
                    .build());
        }

        resultat.sort((a, b) -> Long.compare(b.getTotalOperations(), a.getTotalOperations()));

        return resultat;
    }

    public PersonnelStatDTO getStatistiquesPersonnelById(
            Long personnelId,
            LocalDateTime debut,
            LocalDateTime fin
    ) {
        return getStatistiquesPersonnel(debut, fin)
                .stream()
                .filter(s -> s.getPersonnelId().equals(personnelId))
                .findFirst()
                .orElse(null);
    }
}