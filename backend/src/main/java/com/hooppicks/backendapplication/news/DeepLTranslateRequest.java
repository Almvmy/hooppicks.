package com.hooppicks.backendapplication.news;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DeepLTranslateRequest(
        List<String> text,
        @JsonProperty("target_lang") String targetLang,
        @JsonProperty("source_lang") String sourceLang
) {
}
