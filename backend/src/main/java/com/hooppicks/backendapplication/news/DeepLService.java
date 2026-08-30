package com.hooppicks.backendapplication.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class DeepLService {

    private static final Logger log = LoggerFactory.getLogger(DeepLService.class);

    private final RestTemplate restTemplate;

    @Value("${deepl.api-key:}")
    private String apiKey;

    @Value("${deepl.api-url:https://api-free.deepl.com/v2/translate}")
    private String apiUrl;

    public DeepLService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Traduit une liste de textes EN -> FR en un seul appel (DeepL accepte
     * plusieurs "text" par requête). Si la clé n'est pas configurée ou que
     * l'appel échoue, renvoie les textes originaux tels quels : l'app doit
     * rester utilisable (en anglais) plutôt que de casser la page actus.
     */
    public List<String> translateToFrench(List<String> texts) {
        if (apiKey == null || apiKey.isBlank() || texts.isEmpty()) {
            return texts;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DeepLTranslateRequest> entity =
                new HttpEntity<>(new DeepLTranslateRequest(texts, "FR", "EN"), headers);

        // Même instabilité réseau observée que pour ESPN (cf. NewsService) :
        // même remède : quelques tentatives espacées plutôt qu'un abandon
        // immédiat sur une IP malchanceuse.
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                DeepLResponse response = restTemplate.postForObject(apiUrl, entity, DeepLResponse.class);
                if (response == null || response.translations() == null
                        || response.translations().size() != texts.size()) {
                    return texts;
                }
                return response.translations().stream().map(DeepLResponse.Translation::text).toList();
            } catch (Exception e) {
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
        return texts;
    }
}
