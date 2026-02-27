package pharmacie.service;

import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import pharmacie.dao.CategorieRepository;
import pharmacie.dao.FournisseurRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.mail.host=localhost",
    "spring.mail.port=3025",
    "spring.mail.protocol=smtp"
})
public class ApprovisionnementEmailIntegrationTest {

    private GreenMail greenMail;

    @Autowired
    private ApprovisionnementService approvisionnementService;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @BeforeEach
    void startSmtp() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterEach
    void stopSmtp() {
        if (greenMail != null) greenMail.stop();
    }

    @Test
    void envoie_mail_au_fournisseur_quand_medicament_trop_bas() throws Exception {
        // Préparer les données : catégorie, fournisseur, médicament lié au fournisseur
        Categorie cat = new Categorie();
        cat.setLibelle("cat-test-integ");
        cat = categorieRepository.save(cat);

        Fournisseur f = new Fournisseur();
        f.setNom_fournisseur("Fournisseur Test");
        // adresse locale capturée par GreenMail
        f.setMail_fournisseur("dest@example.com");
        f = fournisseurRepository.save(f);

        Medicament med = new Medicament();
        med.setNom("MedTest-Integ");
        med.setCategorie(cat);
        med.setUnitesEnStock(1); // faible
        med.setNiveauDeReappro(10);
        med = medicamentRepository.save(med);

        // établir la relation sur le côté propriétaire (Fournisseur.medicaments)
        f.getMedicaments().add(med);
        fournisseurRepository.save(f);

        // Exécuter la logique de réapprovisionnement
        approvisionnementService.gererReapprovisionnement();

        // Attendre et vérifier qu'un email a été reçu
        boolean arrived = greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] msgs = greenMail.getReceivedMessages();
        assertTrue(arrived, "Aucun email reçu par GreenMail");
        assertEquals(1, msgs.length, "Doit recevoir exactement 1 email");

        String body = GreenMailUtil.getBody(msgs[0]);
        assertTrue(body.contains("MedTest-Integ"), "Le corps du mail doit contenir le nom du médicament");
    }
}
