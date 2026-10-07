package com.hooppicks.backendapplication.duel;

import com.hooppicks.backendapplication.entity.Duel;
import com.hooppicks.backendapplication.entity.DuelStatus;
import com.hooppicks.backendapplication.entity.User;

import java.time.LocalDate;
import java.util.Map;

/**
 * Un duel vu par l'un des deux joueurs : « moi » et « l'adversaire », quel
 * que soit celui qui a lancé le défi. Points en direct pour un duel de la
 * semaine en cours, figés pour un duel terminé, null sinon.
 */
public record DuelDto(
        String id,
        String status,
        boolean iChallenged,
        String week,
        String opponentUsername,
        int opponentAvatarNumber,
        String opponentAvatarPosition,
        String opponentAvatarColorway,
        String opponentAvatarIcon,
        Long myPoints,
        Long opponentPoints,
        /** "won", "lost", "tie" pour un duel terminé, sinon null. */
        String result,
        String createdAt
) {
    static DuelDto from(Duel d, String viewerId, Map<String, Long> live, LocalDate currentWeek) {
        boolean iChallenged = d.getChallenger().getId().equals(viewerId);
        User other = iChallenged ? d.getOpponent() : d.getChallenger();
        Long mine = null, theirs = null;
        String result = null;
        if (d.getStatus() == DuelStatus.FINISHED) {
            mine = iChallenged ? d.getChallengerPoints() : d.getOpponentPoints();
            theirs = iChallenged ? d.getOpponentPoints() : d.getChallengerPoints();
            result = d.getWinnerId() == null ? "tie" : d.getWinnerId().equals(viewerId) ? "won" : "lost";
        } else if (d.getStatus() == DuelStatus.ACCEPTED && d.getWeek().equals(currentWeek)) {
            mine = live.getOrDefault(viewerId, 0L);
            theirs = live.getOrDefault(other.getId(), 0L);
        }
        return new DuelDto(d.getId(), d.getStatus().name().toLowerCase(), iChallenged, d.getWeek().toString(),
                other.getUsername(), other.getAvatarNumber(), other.getAvatarPosition(), other.getAvatarColorway(),
                other.getAvatarIcon(), mine, theirs, result, d.getCreatedAt().toString());
    }
}
