package com.hooppicks.backendapplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Sans timeout explicite, un appel externe qui ne répond pas (ESPN,
        // balldontlie...) peut bloquer le thread appelant indéfiniment :
        // vécu en pratique avec un "Connection timed out" qui prenait
        // plusieurs minutes à se déclencher tout seul.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        return new RestTemplate(factory);
    }
}