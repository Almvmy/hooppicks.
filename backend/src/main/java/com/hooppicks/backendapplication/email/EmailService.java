package com.hooppicks.backendapplication.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Distinct du login SMTP (spring.mail.username, qui est un identifiant
    // d'authentification Brevo, pas une adresse d'envoi valable) : l'adresse
    // affichée comme expéditeur doit être un email vérifié dans Brevo.
    @Value("${app.mail-from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("HoopPicks — Réinitialisation de ton mot de passe");
        message.setText(
                "Salut " + username + ",\n\n"
                        + "Tu as demandé à réinitialiser ton mot de passe HoopPicks.\n"
                        + "Clique sur ce lien pour en choisir un nouveau (valable 30 minutes) :\n\n"
                        + resetLink + "\n\n"
                        + "Si tu n'es pas à l'origine de cette demande, ignore cet e-mail — ton mot de passe reste inchangé."
        );
        mailSender.send(message);
    }

    public void sendVerificationEmail(String to, String username, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("HoopPicks — Confirme ton adresse e-mail");
        message.setText(
                "Salut " + username + ",\n\n"
                        + "Bienvenue sur HoopPicks ! Confirme ton adresse e-mail en cliquant sur ce lien (valable 24 heures) :\n\n"
                        + verifyLink + "\n\n"
                        + "Si tu n'es pas à l'origine de cette inscription, ignore simplement cet e-mail."
        );
        mailSender.send(message);
    }
}
