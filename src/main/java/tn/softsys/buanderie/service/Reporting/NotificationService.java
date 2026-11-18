package tn.softsys.buanderie.service.Reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.NotificationDTO;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.Notification;
import tn.softsys.buanderie.entity.Reclamation;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;



    public List<Notification> getAll() {

        return notificationRepository
                .findAllByOrderByDateCreationDesc();
    }


    public void notifierUniformeStocke(Linge linge) {

        Notification notification = Notification.builder()
                .titre("Stock uniforme disponible")
                .message(
                        "Un uniforme propre est disponible en stock : "
                                + linge.getRfidTag()
                )
                .type("STOCK_UNIFORME")
                .dateCreation(LocalDateTime.now())
                .lingeId(linge.getId())
                .codeBarre(linge.getCodeBarre())
                .categorie(linge.getCategorie().name())
                .lue(false)
                .build();

        envoyer(notification);
    }




    public void notifierFinDeVie(Linge linge) {

        Notification notification = Notification.builder()
                .titre("Fin de vie du linge")
                .message(
                        "Le linge " + linge.getCodeBarre()
                                + " a atteint sa durée de vie maximale "
                                + "et doit être remplacé."
                )
                .type("FIN_DE_VIE")
                .dateCreation(LocalDateTime.now())
                .lingeId(linge.getId())
                .codeBarre(linge.getCodeBarre())
                .categorie(linge.getCategorie().name())
                .lue(false)
                .build();

        envoyer(notification);
    }


    public void notifierReclamationMachine(tn.softsys.buanderie.entity.Machine machine, Reclamation reclamation) {

        Notification notification = Notification.builder()
                .titre("Réclamation machine")
                .message("Réclamation sur la machine "
                        + (machine.getDesignation() != null ? machine.getDesignation() : machine.getCode())
                        + " : " + reclamation.getType().name())
                .type("RECLAMATION")
                .dateCreation(LocalDateTime.now())
                .codeBarre(machine.getCode())
                .lue(false)
                .build();

        envoyer(notification);
    }

    public void notifierReclamation(Linge linge, Reclamation reclamation) {

        Notification notification = Notification.builder()
                .titre("Nouvelle réclamation")
                .message(
                        "Une réclamation a été créée pour le linge "
                                + linge.getCodeBarre()
                                + " : "
                                + reclamation.getType().name()
                )
                .type("RECLAMATION")
                .dateCreation(LocalDateTime.now())
                .lingeId(linge.getId())
                .codeBarre(linge.getCodeBarre())
                .categorie(linge.getCategorie().name())
                .lue(false)
                .build();

        envoyer(notification);
    }


    @Transactional
    public void notifierLingesIncomplets(int total, Map<String, Long> parAttribut, List<String> exemples) {

        List<Notification> anciennes =
                notificationRepository
                        .findByLueFalseOrderByDateCreationDesc()
                        .stream()
                        .filter(n -> "LINGES_INCOMPLETS".equals(n.getType()))
                        .collect(Collectors.toList());

        anciennes.forEach(n -> n.setLue(true));
        notificationRepository.saveAll(anciennes);

        String detail = parAttribut.entrySet()
                .stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> libelleAttribut(e.getKey()) + " : " + e.getValue())
                .collect(Collectors.joining(", "));

        String message = total + " linge(s) avec des attributs manquants (" + detail + ")."
                + (exemples.isEmpty() ? "" : " Ex. : " + String.join(", ", exemples));

        if (message.length() > 500) {
            message = message.substring(0, 497) + "...";
        }

        Notification notification = Notification.builder()
                .titre("Linges incomplets")
                .message(message)
                .type("LINGES_INCOMPLETS")
                .dateCreation(LocalDateTime.now())
                .lue(false)
                .build();

        envoyer(notification);
    }

    private String libelleAttribut(String attribut) {
        return switch (attribut) {
            case "POIDS" -> "sans poids";
            case "DUREE_VIE" -> "sans durée de vie";
            case "CATEGORIE" -> "sans catégorie";
            case "SERVICE" -> "sans service";
            case "DESIGNATION" -> "sans désignation";
            default -> attribut;
        };
    }


    public void notifierLingesBloques(int total, Map<String, Long> parEtat, List<String> exemples) {

        List<Notification> anciennes =
                notificationRepository
                        .findByLueFalseOrderByDateCreationDesc()
                        .stream()
                        .filter(n -> "LINGES_BLOQUES".equals(n.getType()))
                        .collect(Collectors.toList());

        anciennes.forEach(n -> n.setLue(true));
        notificationRepository.saveAll(anciennes);

        String detail = parEtat.entrySet()
                .stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> ("LIVRE".equals(e.getKey())
                        ? "livrés non rangés (manquants possibles)"
                        : e.getKey().toLowerCase().replace('_', ' ')) + " : " + e.getValue())
                .collect(Collectors.joining(", "));

        String message = total + " linge(s) bloqué(s) dans le circuit (" + detail + ")."
                + (exemples.isEmpty() ? "" : " Ex. : " + String.join(", ", exemples));

        if (message.length() > 500) {
            message = message.substring(0, 497) + "...";
        }

        Notification notification = Notification.builder()
                .titre("Linges bloqués")
                .message(message)
                .type("LINGES_BLOQUES")
                .dateCreation(LocalDateTime.now())
                .lue(false)
                .build();

        envoyer(notification);
    }

    private void envoyer(Notification notification) {

        // Sauvegarde en base
        Notification saved =
                notificationRepository.save(notification);

        //  Préparer DTO pour WebSocket
        NotificationDTO dto = new NotificationDTO(
                saved.getTitre(),
                saved.getMessage(),
                saved.getType(),
                saved.getDateCreation(),
                saved.getLingeId(),
                saved.getCodeBarre(),
                saved.getCategorie()
        );

        // 3. Envoi temps réel
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                dto
        );
    }

    public void notifierLingeBloque(Linge linge, long joursBloque) {

        boolean dejaNotifie =
                notificationRepository
                        .existsByLingeIdAndTypeAndLueFalse(
                                linge.getId(),
                                "LINGE_BLOQUE"
                        );

        if (dejaNotifie) {
            return;
        }

        Notification notification = Notification.builder()
                .titre("Linge bloqué")
                .message(
                        "Le linge " + linge.getCodeBarre()
                                + " est bloqué en "
                                + linge.getEtat().name()
                                + " depuis "
                                + joursBloque
                                + " jours."
                )
                .type("LINGE_BLOQUE")
                .dateCreation(LocalDateTime.now())
                .lingeId(linge.getId())
                .codeBarre(linge.getCodeBarre())
                .categorie(
                        linge.getCategorie() != null
                                ? linge.getCategorie().name()
                                : null
                )
                .lue(false)
                .build();

        envoyer(notification);
    }
    @Transactional
    public Notification lire(Long id) {

        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Notification",
                                        "id",
                                        id
                                )
                        );

        notification.setLue(true);

        return notificationRepository.save(notification);
    }
    @Transactional
    public void lireToutes() {

        List<Notification> notifications =
                notificationRepository
                        .findByLueFalseOrderByDateCreationDesc();

        notifications.forEach(notification ->
                notification.setLue(true)
        );

        notificationRepository.saveAll(notifications);
    }
}