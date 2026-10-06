package com.hooppicks.backendapplication.entity;

/**
 * Phase de la saison à laquelle appartient un match, lue sur ESPN (balldontlie
 * ne distingue que "playoffs ou pas" et ne connaît ni la présaison, ni la
 * Coupe NBA, ni l'All-Star Game). Null tant que le match n'a pas été relié à
 * son event ESPN.
 */
public enum MatchType {
    PRESEASON, REGULAR, NBA_CUP, ALL_STAR, PLAY_IN, PLAYOFFS
}
