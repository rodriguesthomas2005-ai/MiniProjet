package pharmacie.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pharmacie.dao.DispensaireRepository;

@SpringBootTest
 // Ce test est basé sur le jeu de données dans "test_data.sql"
class CreationCommandeTest {
    // fournir un bean JavaMailSender mock pour que le contexte se charge (utilisé par
    // ApprovisionnementService/Controller qui est présent dans l'application)
    @org.springframework.boot.test.context.TestConfiguration
    static class MailConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.mail.javamail.JavaMailSender javaMailSender() {
            return org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        }
    }
    private static final String ID_PETIT_CLIENT = "0COM";
    private static final String ID_GROS_CLIENT = "2COM";
    private static final BigDecimal REMISE_POUR_GROS_CLIENT = new BigDecimal("0.15");

    @Autowired
    private CommandeService service;
    @Autowired
    private DispensaireRepository daoClient;

    @Test
    void testCreerCommandePourGrosClient() {
        var commande = service.creerCommande(ID_GROS_CLIENT);
        assertNotNull(commande.getNumero(), "On doit avoir la clé de la commande");
        assertEquals(REMISE_POUR_GROS_CLIENT, commande.getRemise(),
            "Une remise de 15% doit être appliquée pour les gros clients");
    }

    @Test
    void testCreerCommandePourPetitClient() {
        var commande = service.creerCommande(ID_PETIT_CLIENT);
        assertNotNull(commande.getNumero());
        assertEquals(BigDecimal.ZERO, commande.getRemise(),
            "Aucune remise ne doit être appliquée pour les petits clients");
    }

    @Test
    void testCreerCommandeInitialiseAdresseLivraison() {
        var commande = service.creerCommande(ID_PETIT_CLIENT);
        var client = daoClient.findById(ID_PETIT_CLIENT).orElseThrow();
        assertEquals(client.getAdresse(), commande.getAdresseLivraison(),
            "On doit recopier l'adresse du client dans l'adresse de livraison");
    }
}
