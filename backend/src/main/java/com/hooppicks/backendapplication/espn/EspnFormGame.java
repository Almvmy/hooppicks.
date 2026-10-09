package com.hooppicks.backendapplication.espn;

import java.time.Instant;

/** Un des derniers matchs d'une équipe, vu de son côté (sigles balldontlie). */
public record EspnFormGame(Instant date, String opponent, boolean home, int teamScore, int opponentScore, boolean won) {
}
