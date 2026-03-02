package pharmacie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import jakarta.mail.MessagingException;
import pharmacie.dao.CategorieRepository;
import pharmacie.dao.CommandeRepository;
import pharmacie.dao.MedicamentRepository;

@SpringBootTest
@ActiveProfiles("test") // utilise application.properties de test
class CommandeServiceReapproTest {

    @TestConfiguration
    static class MockApproConfig {
        @Bean
        public ApprovisionnementService approvisionnementService() throws MessagingException {
            // on renvoie un mock capable de ne rien faire lorsqu'on l'appelle
            ApprovisionnementService mock = org.mockito.Mockito.mock(ApprovisionnementService.class);
            return mock;
        }
        // Le bean JavaMailSender est créé par MailConfig dans l'application normale.
        // Il n'est pas nécessaire dans ce test (ApprovisionnementService est mocké).
        // Nous ne déclarons donc pas de bean ici pour éviter un conflit de définition.
    }

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private ApprovisionnementService approvisionnementService;

    @Autowired
    private MedicamentRepository medicamentRepository;

    // needed for creating a valid Medicament in tests
    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @BeforeEach
    void setUp() {
        // create and persist a category so the medicament satisfies the not-null FK
        var cat = new pharmacie.entity.Categorie();
        cat.setLibelle("cat-test");
        cat = categorieRepository.save(cat);

        var med = new pharmacie.entity.Medicament();
        med.setNom("test");
        med.setCategorie(cat);
        med.setUnitesEnStock(5);
        med.setUnitesCommandees(0);
        med.setNiveauDeReappro(10);
        med = medicamentRepository.save(med);

        var cmd = commandeService.creerCommande("0COM");
        commandeService.ajouterLigne(cmd.getNumero(), med.getReference(), 3);
    }

    @Test
    void expeditionDeclencheReappro() throws Exception {
        var commande = commandeRepository.findAll().get(0);
        commandeService.enregistreExpedition(commande.getNumero());
        verify(approvisionnementService).gererReapprovisionnement();
    }
}
