package com.hooppicks.backendapplication.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaGameListResponse(List<NbaGameDto> data, Meta meta) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(@JsonProperty("next_cursor") Long nextCursor) {}
}
