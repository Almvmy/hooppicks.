package com.hooppicks.backendapplication.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Envoi via l'API HTTP de Brevo plutôt que le SMTP classique : Railway (et
 * pas mal d'hébergeurs PaaS) bloque les ports SMTP sortants (587/465) pour
 * limiter le spam — timeout systématique constaté en prod. L'API HTTP passe
 * par le port 443 comme n'importe quel appel externe (balldontlie, DeepL),
 * donc jamais bloquée de la même façon.
 */
@Service
public class EmailService {

    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    // Distinct du login SMTP historique : l'adresse affichée comme expéditeur
    // doit être un email vérifié dans Brevo.
    @Value("${app.mail-from}")
    private String fromAddress;

    public EmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        send(to, "HoopPicks — Réinitialisation de ton mot de passe",
                "Salut " + username + ",\n\n"
                        + "Tu as demandé à réinitialiser ton mot de passe HoopPicks.\n"
                        + "Clique sur ce lien pour en choisir un nouveau (valable 30 minutes) :\n\n"
                        + resetLink + "\n\n"
                        + "Si tu n'es pas à l'origine de cette demande, ignore cet e-mail — ton mot de passe reste inchangé."
        );
    }

    public void sendVerificationEmail(String to, String username, String verifyLink) {
        send(to, "HoopPicks — Confirme ton adresse e-mail",
                "Salut " + username + ",\n\n"
                        + "Bienvenue sur HoopPicks ! Confirme ton adresse e-mail en cliquant sur ce lien (valable 24 heures) :\n\n"
                        + verifyLink + "\n\n"
                        + "Si tu n'es pas à l'origine de cette inscription, ignore simplement cet e-mail."
        );
    }

    private void send(String to, String subject, String textContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        BrevoEmailRequest body = new BrevoEmailRequest(
                new BrevoEmailRequest.Contact(fromAddress, "HoopPicks"),
                List.of(new BrevoEmailRequest.Contact(to, null)),
                subject,
                textContent
        );

        // Laisse l'exception remonter : les deux appelants (reset mot de passe,
        // vérification email) attrapent déjà l'échec pour ne jamais bloquer le
        // flux principal — même comportement qu'avec l'ancien JavaMailSender.
        restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), String.class);
    }

    private record BrevoEmailRequest(Contact sender, List<Contact> to, String subject, String textContent) {
        private record Contact(String email, String name) {}
    }
}
