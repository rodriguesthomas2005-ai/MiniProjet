package pharmacie.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import pharmacie.service.ApprovisionnementService;

@RestController
@RequestMapping("/api/approvisionnement")
public class ApprovisionnementRestController {

    @Autowired
    private ApprovisionnementService approvisionnementService;

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerApprovisionnement() {
        try {
            approvisionnementService.gererReapprovisionnement();
            return ResponseEntity.ok("Emails d'approvisionnement envoyés (si présents)");
        } catch (MessagingException e) {
            return ResponseEntity.status(500).body("Erreur envoi mail: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de l'opération d'approvisionnement: " + e.getMessage());
        }
    }
}
