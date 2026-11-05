package com.hooppicks.backendapplication.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepLService {

    private static final Logger log = LoggerFactory.getLogger(DeepLService.class);
    // Limite de DeepL par requête : 30 articles (titre + description) la dépassent.
    static final int BATCH_SIZE = 50;

    private final RestTemplate restTemplate;

    @Value("${deepl.api-key:}")
    private String apiKey;

    @Value("${deepl.api-url:https://api-free.deepl.com/v2/translate}")
    private String apiUrl;

    public DeepLService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Textes traduits (anglais → français) et, si tout n'a pas pu l'être, la
     * raison. Un texte non traduit est absent de `translations` : le renvoyer
     * tel quel le ferait passer pour traduit, et il resterait en anglais une
     * fois DeepL revenu.
     */
    public record Result(Map<String, String> translations, String error) {}

    /** EN → FR par lots. La page actus reste utilisable (en anglais) si la clé manque ou si l'appel échoue. */
    public Result translateToFrench(List<String> texts) {
        Map<String, String> done = new LinkedHashMap<>();
        if (texts.isEmpty()) return new Result(done, null);
        if (apiKey == null || apiKey.isBlank()) return new Result(done, "Clé DeepL absente");

        for (int from = 0; from < texts.size(); from += BATCH_SIZE) {
            List<String> batch = texts.subList(from, Math.min(from + BATCH_SIZE, texts.size()));
            String error = translateBatch(batch, done);
            if (error != null) return new Result(done, error);
        }
        return new Result(done, null);
    }

    private String translateBatch(List<String> batch, Map<String, String> done) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DeepLTranslateRequest> entity =
                new HttpEntity<>(new DeepLTranslateRequest(batch, "FR", "EN"), headers);

        // Même instabilité réseau observée que pour ESPN (cf. NewsService) :
        // quelques tentatives espacées plutôt qu'un abandon sur une IP malchanceuse.
        String error = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                DeepLResponse response = restTemplate.postForObject(apiUrl, entity, DeepLResponse.class);
                if (response == null || response.translations() == null
                        || response.translations().size() != batch.size()) {
                    return "Réponse DeepL incomplète";
                }
                for (int i = 0; i < batch.size(); i++) done.put(batch.get(i), response.translations().get(i).text());
                return null;
            } catch (HttpClientErrorException e) {
                // Quota (456) ou clé refusée (403) : réessayer ne changera rien
                // et bloquerait le rafraîchissement 22 s pour rien.
                int status = e.getStatusCode().value();
                error = status == 456 ? "Quota DeepL dépassé (456)" : "DeepL a refusé l'appel (" + status + ")";
                log.warn("Traduction DeepL refusée : {}", error);
                return error;
            } catch (Exception e) {
                error = "DeepL injoignable : " + e.getMessage();
                log.warn("Traduction DeepL échouée (tentative {}/3) : {}", attempt, e.getMessage());
                if (attempt == 3) break;
                try {
                    Thread.sleep(11_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return error;
    }
}
