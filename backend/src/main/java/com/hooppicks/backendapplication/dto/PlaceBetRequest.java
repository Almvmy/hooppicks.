package com.hooppicks.backendapplication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceBetRequest(
        // 8 sélections au plus : au-delà, un combiné n'est plus un pronostic
        // mais un billet de loterie (et le gain finissait par déborder).
        @NotEmpty @Size(max = MAX_SELECTIONS, message = "8 sélections au maximum par ticket.") List<@Valid SelectionInput> selections,
        // Mise minimum : des tickets à 1 pt sur de gros favoris gonflaient le
        // taux de réussite et les badges sans rien risquer.
        @Min(value = MIN_STAKE, message = "Mise minimum : 10 pts.") int stake
) {
    public static final int MAX_SELECTIONS = 8;
    public static final int MIN_STAKE = 10;

    // matchLabel, label et odds sont ignorés par le serveur (cotes et
    // libellés recalculés depuis le match) : gardés pour ne pas casser les
    // clients déjà déployés.
    public record SelectionInput(
            String matchId,
            String matchLabel,
            String market,
            String outcome,
            String label,
            double odds,
            // Pari joueur seulement : joueur visé et ligne affichée au moment
            // du choix (refus si elle a bougé depuis, cf. BetController).
            String playerId,
            Double line
    ) {}
}