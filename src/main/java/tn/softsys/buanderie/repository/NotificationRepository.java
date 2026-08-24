package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tn.softsys.buanderie.entity.Notification;

import java.util.List;


@EnableJpaRepositories
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findAllByOrderByDateCreationDesc();

    List<Notification> findByLueFalseOrderByDateCreationDesc();
}
