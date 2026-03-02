package pharmacie.service;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;
@Service
public class ApprovisionnementService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovisionnementService.class);

    @Value("${spring.mail.username:contact@pharmacie.example}")
    private String mailFrom;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Transactional
    public void gererReapprovisionnement() throws MessagingException {
        // Étape 1 : Trouver les médicaments à réapprovisionner
        List<Medicament> medicamentsAReapprovisionner = medicamentRepository.findMedicamentsToReorder();
        logger.debug("Médicaments à réapprovisionner : {}", medicamentsAReapprovisionner.stream().map(Medicament::getNom).collect(Collectors.toList()));

        // Étape 2 : Regrouper les médicaments par fournisseur (fournisseurs définis sur la catégorie)
        Map<Fournisseur, List<Medicament>> medicamentsParFournisseur = medicamentsAReapprovisionner.stream()
            .flatMap(medicament -> {
                var fournisseurs = medicament.getCategorie().getFournisseurs();
                logger.debug("Catégorie {} a fournisseurs {}", medicament.getCategorie().getLibelle(),
                    fournisseurs.stream().map(Fournisseur::getNom_fournisseur).collect(Collectors.toList()));
                return fournisseurs.stream().map(fournisseur -> Map.entry(fournisseur, medicament));
            })
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        logger.debug("Médicaments groupés par fournisseur : {}",
            medicamentsParFournisseur.entrySet().stream()
                .map(e -> e.getKey().getNom_fournisseur() + " -> " + e.getValue().stream().map(Medicament::getNom).toList())
                .toList());

        // Étape 3 : Envoyer un email à chaque fournisseur
        for (Map.Entry<Fournisseur, List<Medicament>> entry : medicamentsParFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();
            List<Medicament> medicaments = entry.getValue();
            try {
                envoyerEmailReapprovisionnement(fournisseur, medicaments);
            } catch (MessagingException e) {
                logger.error("Erreur lors de l'envoi de l'email au fournisseur {} : {}", fournisseur.getNom_fournisseur(), e.getMessage(), e);
            }
        }
    }

    /**
     * Envoie un simple message "hello world" à l'adresse fournie, utile pour tester
     * que l'infrastructure mail est correctement configurée.
     */
    public void envoyerMailTest(String destinataire) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(msg, false);
        helper.setTo(destinataire);
        helper.setFrom(Objects.requireNonNull(mailFrom, "spring.mail.username non configuré"));
        helper.setSubject("Test de messagerie");
        helper.setText("Hello world !", false);
        mailSender.send(msg);
        logger.info("Mail de test envoyé à {}", destinataire);
    }

    private void envoyerEmailReapprovisionnement(Fournisseur fournisseur, List<Medicament> medicaments) throws MessagingException {
        // Configuration de l'email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        // Préparer destinataire
        String email = fournisseur.getMail_fournisseur();
        if (email.isBlank()) {
            logger.warn("Fournisseur {} sans adresse email, on saute l'envoi.", fournisseur.getNom_fournisseur());
            return;
        }
        helper.setTo(Objects.requireNonNull(email).trim());

        // Expéditeur et sujet
        helper.setFrom(Objects.requireNonNull(mailFrom, "spring.mail.username non configuré"));
        helper.setSubject("Demande de devis de réapprovisionnement");

        // Vérification explicite pour garantir que le contenu de l'email est non nul
        StringBuilder contenu = new StringBuilder("Bonjour " + fournisseur.getNom_fournisseur() + ",\n\n");
        contenu.append("Nous avons besoin de réapprovisionner les médicaments suivants :\n\n");

        medicaments.stream()
                .collect(Collectors.groupingBy(medicament -> medicament.getCategorie().getNom()))
                .forEach((categorie, meds) -> {
                    contenu.append("Catégorie : ").append(categorie).append("\n");
                    meds.forEach(med -> contenu.append("- ").append(med.getNom()).append(" (Stock actuel : ").append(med.getUnitesEnStock()).append(")\n"));
                    contenu.append("\n");
                });

        contenu.append("\nMerci de nous transmettre un devis pour ces produits.\nCordialement,\nVotre équipe de gestion.");

        // Ajout du contenu à l'email
        helper.setText(Objects.requireNonNull(contenu.toString()), false);
        mailSender.send(message);
    }
}
