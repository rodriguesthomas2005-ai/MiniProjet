package pharmacie.service;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


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


    @Autowired
    private ApprovisionnementService approvisionnementService;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

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
        // lien cat<->fournisseur : le service regarde les fournisseurs attachés à la catégorie
        f.getCategories().add(cat);
        cat.getFournisseurs().add(f);
        fournisseurRepository.save(f);
        categorieRepository.save(cat);
        // re-sauvegarder (ou rafraîchir) le médicament pour que la catégorie associée soit à jour
        med = medicamentRepository.save(med);

        // vérification supplémentaire pour confirmer que le médicament est bien repéré
        var toReorder = medicamentRepository.findMedicamentsToReorder();
        assertEquals(1, toReorder.size(), "Le médicament doit être détecté comme à réapprovisionner");

        // vérifier manuellement le regroupement avant d'appeler le service
        var medsToReorder = medicamentRepository.findMedicamentsToReorder();
        var manualMap = medsToReorder.stream()
            .flatMap(medicament -> medicament.getCategorie().getFournisseurs().stream()
                    .map(fournisseur -> Map.entry(fournisseur, medicament)))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        // debug assertions
        assertFalse(medsToReorder.isEmpty(), "La liste des médicaments à réapprovisionner ne doit pas être vide");
        assertFalse(manualMap.isEmpty(), "La carte fournisseur->médicaments ne doit pas être vide");

        // préparer le mock pour qu'il fournisse un MimeMessage utilisable
        org.mockito.Mockito.when(mailSender.createMimeMessage()).thenReturn(new jakarta.mail.internet.MimeMessage((jakarta.mail.Session)null));

        // Exécuter la logique de réapprovisionnement
        approvisionnementService.gererReapprovisionnement();

        // Vérifier que le JavaMailSender a bien été appelé
        org.mockito.Mockito.verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(org.mockito.Mockito.any(jakarta.mail.internet.MimeMessage.class));
    }
}
