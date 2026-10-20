package com.hooppicks.backendapplication.avatar;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.palmares.PalmaresService;
import com.hooppicks.backendapplication.repository.PhaseTitleRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WeeklyTitleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maillot (avatar) des joueurs. Sans personnalisation, tout le monde avait le
 * même n°0 orange et le classement paraissait uniforme : le maillot par
 * défaut prend désormais les couleurs de l'équipe favorite, avec numéro,
 * poste et style tirés du pseudo (stables d'un affichage à l'autre). Il
 * suit l'équipe favorite tant que le joueur n'a pas touché à son maillot
 * (`User.avatarCustomized`).
 *
 * Icônes à débloquer, une raison de revenir dans l'éditeur : « Couronne »
 * pour les joueurs déjà titrés (champion de la semaine, de la Coupe NBA ou
 * des playoffs, sur tout HoopPicks ou dans une ligue), « Inarrêtable » après
 * 5 tickets gagnés d'affilée (même règle que le badge « Sur un nuage »).
 */
@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    public static final Set<String> POSITIONS = Set.of("PG", "SG", "SF", "PF", "C");
    public static final Set<String> COLORWAYS = Set.of("orange", "purple", "blue", "green", "red", "teal", "gold", "navy", "black");
    public static final Set<String> ICONS = Set.of("dunk", "three", "handles", "defense", "playmaker", "crown", "streak");
    public static final String CROWN = "crown";
    public static final String STREAK = "streak";
    private static final int STREAK_TO_UNLOCK = 5;

    private static final List<String> FREE_ICONS = List.of("dunk", "three", "handles", "defense", "playmaker");
    private static final List<String> POSITION_ORDER = List.of("PG", "SG", "SF", "PF", "C");
    private static final List<String> FALLBACK_COLORWAYS = List.of("orange", "purple", "blue", "green", "red", "teal", "gold", "navy", "black");

    // Couleur dominante de chaque équipe, ramenée aux palettes de l'app.
    private static final Map<String, String> TEAM_COLORWAYS = Map.ofEntries(
            Map.entry("Atlanta Hawks", "red"), Map.entry("Boston Celtics", "green"),
            Map.entry("Brooklyn Nets", "black"), Map.entry("Charlotte Hornets", "teal"),
            Map.entry("Chicago Bulls", "red"), Map.entry("Cleveland Cavaliers", "red"),
            Map.entry("Dallas Mavericks", "blue"), Map.entry("Denver Nuggets", "gold"),
            Map.entry("Detroit Pistons", "red"), Map.entry("Golden State Warriors", "blue"),
            Map.entry("Houston Rockets", "red"), Map.entry("Indiana Pacers", "gold"),
            Map.entry("LA Clippers", "blue"), Map.entry("Los Angeles Clippers", "blue"),
            Map.entry("Los Angeles Lakers", "purple"), Map.entry("Memphis Grizzlies", "navy"),
            Map.entry("Miami Heat", "red"), Map.entry("Milwaukee Bucks", "green"),
            Map.entry("Minnesota Timberwolves", "navy"), Map.entry("New Orleans Pelicans", "navy"),
            Map.entry("New York Knicks", "orange"), Map.entry("Oklahoma City Thunder", "blue"),
            Map.entry("Orlando Magic", "blue"), Map.entry("Philadelphia 76ers", "blue"),
            Map.entry("Phoenix Suns", "purple"), Map.entry("Portland Trail Blazers", "red"),
            Map.entry("Sacramento Kings", "purple"), Map.entry("San Antonio Spurs", "black"),
            Map.entry("Toronto Raptors", "red"), Map.entry("Utah Jazz", "purple"),
            Map.entry("Washington Wizards", "navy")
    );

    private final UserRepository userRepository;
    private final WeeklyTitleRepository weeklyTitleRepository;
    private final PhaseTitleRepository phaseTitleRepository;
    private final BetRepository betRepository;
    private final BadgeService badgeService;

    public AvatarService(UserRepository userRepository, WeeklyTitleRepository weeklyTitleRepository,
                         PhaseTitleRepository phaseTitleRepository, BetRepository betRepository,
                         BadgeService badgeService) {
        this.betRepository = betRepository;
        this.badgeService = badgeService;
        this.userRepository = userRepository;
        this.weeklyTitleRepository = weeklyTitleRepository;
        this.phaseTitleRepository = phaseTitleRepository;
    }

    /** Maillot par défaut : couleurs de l'équipe favorite, le reste tiré du pseudo. */
    public static void applyDefaults(User user) {
        int seed = Math.abs((user.getUsername() == null ? "" : user.getUsername().toLowerCase()).hashCode());
        user.setAvatarNumber(1 + seed % 99);
        user.setAvatarPosition(POSITION_ORDER.get((seed / 99) % POSITION_ORDER.size()));
        user.setAvatarIcon(FREE_ICONS.get((seed / 7) % FREE_ICONS.size()));
        user.setAvatarColorway(colorwayFor(user.getFavoriteTeam(), seed));
    }

    static String colorwayFor(String favoriteTeam, int seed) {
        String team = favoriteTeam == null ? "" : favoriteTeam.trim();
        String colorway = TEAM_COLORWAYS.get(team);
        return colorway != null ? colorway : FALLBACK_COLORWAYS.get((seed / 13) % FALLBACK_COLORWAYS.size());
    }

    /** Nouvelle équipe favorite : le maillot jamais personnalisé en prend les couleurs. */
    public static void followFavoriteTeam(User user) {
        if (Boolean.TRUE.equals(user.getAvatarCustomized())) return;
        int seed = Math.abs((user.getUsername() == null ? "" : user.getUsername().toLowerCase()).hashCode());
        user.setAvatarColorway(colorwayFor(user.getFavoriteTeam(), seed));
    }

    /** Icônes réservées que ce joueur a débloquées. */
    @Transactional(readOnly = true)
    public List<String> unlockedIcons(String userId) {
        List<String> unlocked = new java.util.ArrayList<>();
        if (weeklyTitleRepository.existsByUserIdAndWeekGreaterThanEqual(userId, PalmaresService.FIRST_WEEK)
                || phaseTitleRepository.existsByUserId(userId)) {
            unlocked.add(CROWN);
        }
        if (badgeService.computeBestWinStreak(betRepository.findByUserIdOrderByPlacedAtDesc(userId)) >= STREAK_TO_UNLOCK) {
            unlocked.add(STREAK);
        }
        return unlocked;
    }

    public boolean canUseIcon(String userId, String icon) {
        if (!ICONS.contains(icon)) return false;
        return !(CROWN.equals(icon) || STREAK.equals(icon)) || unlockedIcons(userId).contains(icon);
    }

    // Comptes créés avant les maillots par défaut, qui ont tous le même n°0
    // orange : on leur attribue le leur, une fois. Un joueur qui a choisi
    // exactement ce maillot-là (rare) le perd, faute de l'avoir noté avant.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillDefaults() {
        List<User> untouched = userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getAvatarCustomized()))
                .filter(u -> u.getAvatarNumber() == 0 && "PG".equals(u.getAvatarPosition())
                        && "orange".equals(u.getAvatarColorway()) && "dunk".equals(u.getAvatarIcon()))
                .toList();
        if (untouched.isEmpty()) return;
        untouched.forEach(AvatarService::applyDefaults);
        userRepository.saveAll(untouched);
        log.info("Maillots par défaut attribués à {} joueur(s)", untouched.size());
    }
}
