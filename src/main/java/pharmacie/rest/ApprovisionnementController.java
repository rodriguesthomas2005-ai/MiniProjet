package pharmacie.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pharmacie.service.ApprovisionnementService;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/api/approvisionnement")
public class ApprovisionnementController {

    @Autowired
    private ApprovisionnementService approvisionnementService;

    @GetMapping("/reapprovisionner")
    public ResponseEntity<String> reapprovisionner() {
        try {
            approvisionnementService.gererReapprovisionnement();
            return ResponseEntity.ok("Réapprovisionnement effectué avec succès et emails envoyés.");
        } catch (MessagingException e) {
            return ResponseEntity.status(500).body("Erreur lors de l'envoi des emails : " + e.getMessage());
        }
    }
}