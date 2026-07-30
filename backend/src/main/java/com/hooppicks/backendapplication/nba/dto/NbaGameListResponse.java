package com.hooppicks.backendapplication.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaGameListResponse(List<NbaGameDto> data) {}