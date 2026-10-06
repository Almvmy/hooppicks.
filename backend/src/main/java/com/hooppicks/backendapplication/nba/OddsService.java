package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calcule des cotes plausibles à partir de la force Elo des deux équipes
 * (cf. EloService). L'API gratuite ne fournit pas de vraies cotes : celles-ci
 * sont donc synthétiques mais varient selon les équipes, contrairement aux
 * anciennes cotes fixes identiques pour tous les matchs.
 */
@Service
public class OddsService {

    private static final double HOME_ADVANTAGE = 100.0; // avantage terrain, en points Elo
    private static final double VIG = 1.06; // marge bookmaker ~6%, répartie sur les deux côtés
    private static final double ELO_POINTS_PER_MARGIN_POINT = 28.0; // ~1 pt d'écart au score par 28 Elo
    // Moyenne de points par équipe et par match en NBA ces dernières saisons
    // (~225-230 points par match à deux). Sert de point de départ quand une
    // équipe a peu de matchs joués. L'ancienne base fixe de 220,5 donnait la
    // même ligne à tous les matchs, nettement sous la réalité : parier « plus
    // de » à chaque match devenait un gain quasi assuré à long terme.
    static final double LEAGUE_POINTS_PER_TEAM = 114.0;
    // Matchs récents pris en compte, et poids de la moyenne de la ligue
    // (équivalent à 5 matchs) : avec 2 matchs joués, la ligue pèse encore
    // plus que l'équipe ; avec 10, l'équipe domine.
    private static final int RECENT_GAMES = 10;
    private static final double PRIOR_GAMES = 5.0;
    // Une cote sous 1,00 ferait perdre des points sur un pari gagnant.
    private static final double MIN_ODDS = 1.03;

    private final MatchRepository matchRepository;

    public OddsService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Calcule et pose les cotes (moneyline, spread, total) sur le match à
     * partir des Elo actuels des deux équipes. N'appeler que sur un match
     * SCHEDULED sans pari en attente dessus (cf. NbaSyncService) : sinon on
     * risque de faire bouger une ligne sur laquelle un pari a déjà été posé.
     */
    public void applyOdds(Match match, Team home, Team away) {
        double homeElo = home.getEloRating();
        double awayElo = away.getEloRating();

        double probHome = clamp(
                1.0 / (1.0 + Math.pow(10, (awayElo - (homeElo + HOME_ADVANTAGE)) / 400.0)),
                0.05, 0.95
        );
        double probAway = 1.0 - probHome;

        match.setMoneylineHome(Math.max(MIN_ODDS, round2(1.0 / (probHome * VIG))));
        match.setMoneylineAway(Math.max(MIN_ODDS, round2(1.0 / (probAway * VIG))));

        double eloDiff = (homeElo + HOME_ADVANTAGE) - awayElo;
        double spread = clamp(-eloDiff / ELO_POINTS_PER_MARGIN_POINT, -20, 20);
        match.setSpreadValue(roundHalf(spread));
        match.setSpreadOddsHome(1.91);
        match.setSpreadOddsAway(1.91);

        // Points attendus de chaque équipe : moyenne de son attaque et de la
        // défense adverse sur leurs derniers matchs. Toujours en x,5 : pas
        // d'égalité pile possible sur un total.
        TeamForm h = form(home);
        TeamForm a = form(away);
        double total = (h.scored() + a.conceded()) / 2.0 + (a.scored() + h.conceded()) / 2.0;
        match.setTotalValue(Math.floor(clamp(total, 190, 260)) + 0.5);
        match.setTotalOddsOver(1.91);
        match.setTotalOddsUnder(1.91);
    }

    private record TeamForm(double scored, double conceded) {}

    // Hors présaison : rotations et temps de jeu n'y sont pas représentatifs.
    TeamForm form(Team team) {
        if (team == null || team.getId() == null) return new TeamForm(LEAGUE_POINTS_PER_TEAM, LEAGUE_POINTS_PER_TEAM);
        List<Match> games = matchRepository.findRecentFinishedForTeam(team, MatchType.PRESEASON, PageRequest.of(0, RECENT_GAMES));
        double scored = 0, conceded = 0;
        for (Match m : games) {
            boolean isHome = team.getId().equals(m.getHomeTeam().getId());
            scored += isHome ? m.getHomeScore() : m.getAwayScore();
            conceded += isHome ? m.getAwayScore() : m.getHomeScore();
        }
        double prior = PRIOR_GAMES * LEAGUE_POINTS_PER_TEAM;
        double n = games.size() + PRIOR_GAMES;
        return new TeamForm((scored + prior) / n, (conceded + prior) / n);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double roundHalf(double value) {
        return Math.round(value * 2.0) / 2.0;
    }
}
