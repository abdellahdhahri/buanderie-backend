package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.Notification;
import tn.softsys.buanderie.service.Reporting.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;


    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {

        return ResponseEntity.ok(
                notificationService.getAll()
        );
    }


    @PutMapping("/{id}/lire")
    public ResponseEntity<Notification> lire(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                notificationService.lire(id)
        );
    }



    @PutMapping("/lire-toutes")
    public ResponseEntity<Void> lireToutes() {

        notificationService.lireToutes();

        return ResponseEntity.noContent().build();
    }
}