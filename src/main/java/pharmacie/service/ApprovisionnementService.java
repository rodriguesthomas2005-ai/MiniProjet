package pharmacie.service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;
@Service
public class ApprovisionnementService {

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void gererReapprovisionnement() throws MessagingException {
        // Étape 1 : Trouver les médicaments à réapprovisionner
        List<Medicament> medicamentsAReapprovisionner = medicamentRepository.findByUnitesEnStockLessThanNiveauDeReappro();

        // Étape 2 : Regrouper les médicaments par fournisseur
        Map<Fournisseur, List<Medicament>> medicamentsParFournisseur = medicamentsAReapprovisionner.stream()
                .flatMap(medicament -> medicament.getFournisseurs().stream()
                        .map(fournisseur -> Map.entry(fournisseur, medicament)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        // Étape 3 : Envoyer un email à chaque fournisseur
        for (Map.Entry<Fournisseur, List<Medicament>> entry : medicamentsParFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();
            List<Medicament> medicaments = entry.getValue();
            envoyerEmailReapprovisionnement(fournisseur, medicaments);
        }
    }

    private void envoyerEmailReapprovisionnement(Fournisseur fournisseur, List<Medicament> medicaments) throws MessagingException {
        // Configuration de l'email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        // Vérification explicite pour garantir que l'email est non nul et non vide
        String email = fournisseur.getMail_fournisseur();
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("L'adresse email du fournisseur est invalide ou manquante.");
        }
        helper.setTo(email);

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
        helper.setText(contenu.toString(), true);
        mailSender.send(message);
    }
}
