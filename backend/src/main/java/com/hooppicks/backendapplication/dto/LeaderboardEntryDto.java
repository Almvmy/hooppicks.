package com.hooppicks.backendapplication.dto;

import java.util.ArrayList;
import java.util.List;

public record LeaderboardEntryDto(
        int rank,
        String username,
        int points,
        int winRate,
        int totalBets,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon,
        String favoriteTeam,
        // Évolution du rang depuis la veille (+3 = gagné 3 places), null si
        // pas encore d'historique ; newcomer = absent de la photo de la veille.
        Integer rankChange,
        boolean newcomer,
        // 5 derniers tickets, du plus récent au plus ancien ("W" / "L"), et
        // série en cours (+4 = 4 gagnés d'affilée, -2 = 2 perdus).
        List<String> recentForm,
        int streak
) {

    /** Compléments calculés à part (historique, forme), par id utilisateur. */
    public record Extras(Integer rankChange, boolean newcomer, List<String> recentForm, int streak) {
        public static final Extras NONE = new Extras(null, false, List.of(), 0);
    }

    public static List<LeaderboardEntryDto> fromRows(List<Object[]> rows) {
        return fromRows(rows, userId -> Extras.NONE);
    }

    /**
     * Lignes brutes de BetRepository.getLeaderboardRaw / getLeaderboardRawForUsers
     * (déjà triées par points décroissants) -> classement. Partagé par le
     * classement général et celui des ligues, qui dupliquaient ce code.
     *
     * Rang « sportif » : deux joueurs à égalité de points partagent le même
     * rang et le suivant saute d'autant (1, 2, 2, 4), au lieu de rangs
     * différents attribués dans un ordre arbitraire.
     */
    public static List<LeaderboardEntryDto> fromRows(List<Object[]> rows,
                                                     java.util.function.Function<String, Extras> extrasByUserId) {
        List<LeaderboardEntryDto> result = new ArrayList<>();
        int rank = 0;
        long previousPoints = -1;
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            long points = (Long) row[2];
            if (points != previousPoints) {
                rank = i + 1;
                previousPoints = points;
            }
            long totalBets = (Long) row[3];
            long wonBets = (Long) row[4];
            int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);
            String favoriteTeam = (String) row[9];
            Extras extras = extrasByUserId.apply((String) row[0]);

            result.add(new LeaderboardEntryDto(rank, (String) row[1], (int) points, winRate, (int) totalBets,
                    (Integer) row[5], (String) row[6], (String) row[7], (String) row[8],
                    favoriteTeam == null || favoriteTeam.isBlank() ? null : favoriteTeam,
                    extras.rankChange(), extras.newcomer(), extras.recentForm(), extras.streak()));
        }
        return result;
    }
}
