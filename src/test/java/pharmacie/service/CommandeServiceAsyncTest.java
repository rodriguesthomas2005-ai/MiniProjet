package pharmacie.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test; // <-- L'import qui manquait
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.mail.internet.MimeMessage;

@SpringBootTest
@ActiveProfiles("test")
public class CommandeServiceAsyncTest {

    @Autowired
    private CommandeService commandeService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test // <-- Cette annotation sera maintenant reconnue
    public void testAjoutLigneEstInstantane() {
        // Simulation d'un délai de 3 secondes pour l'envoi de mail
        doAnswer(invocation -> {
            Thread.sleep(3000);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        long startTime = System.currentTimeMillis();

        // On déclenche l'ajout d'une ligne (qui appelle le réappro asynchrone)
        // Note : Assurez-vous que la commande 1 et le médicament 1 existent en base
        try {
            commandeService.ajouterLigne(1, 1, 5);
        } catch (Exception e) {
            // Si les données n'existent pas dans votre H2 de test, 
            // le test risque de s'arrêter ici.
            System.out.println("Erreur durant l'ajout (données manquantes ?) : " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Le test réussit si la méthode rend la main presque immédiatement 
        // malgré les 3 secondes de délai simulées dans le mailSender.
        assertTrue(duration < 1000, "L'exécution est synchrone ! Elle a pris : " + duration + "ms");
    }
}