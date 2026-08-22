package com.hooppicks.backendapplication.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeepLResponse(List<Translation> translations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Translation(String text) {
    }
}
