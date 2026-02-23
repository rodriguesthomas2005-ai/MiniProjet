package pharmacie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void envoyerEmail(String destinataire, String sujet, String message) {
        // Créer un objet SimpleMailMessage
        SimpleMailMessage email = new SimpleMailMessage();

        // Définir le destinataire
        email.setTo(destinataire);

        // Définir l'expéditeur (optionnel, mais recommandé)
        email.setFrom("votre_adresse_email@gmail.com");

        // Définir le sujet
        email.setSubject(sujet);

        // Définir le contenu du message
        email.setText(message);

        // Envoyer l'email
        mailSender.send(email);
    }
}