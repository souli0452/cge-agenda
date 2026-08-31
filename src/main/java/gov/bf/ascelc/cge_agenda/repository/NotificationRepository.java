package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByDestinataireEmailIgnoreCaseOrderByCreatedAtDesc(String destinataireEmail);

    long countByDestinataireEmailIgnoreCaseAndLueFalse(String destinataireEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataireEmail = :email AND n.lue = false")
    void marquerToutesLues(@Param("email") String email);
}
