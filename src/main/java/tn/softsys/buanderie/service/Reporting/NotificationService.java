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