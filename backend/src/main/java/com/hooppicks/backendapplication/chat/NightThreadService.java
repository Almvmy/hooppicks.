package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.*;
import com.hooppicks.backendapplication.social.SocialNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Le fil de la soirée NBA. Ouvert à tous les joueurs, donc gardé dès le départ :
 * 280 caractères, pas de liens (la première source de spam), un message toutes
 * les 3 secondes (10, c'était trop long pour réagir à un match), jamais deux fois
 * le même d'affilée, un ticket déjà posé pour écrire (écarte les comptes jetables),
 * mots refusés (liste tenue dans la console), signalement (3 joueurs différents
 * masquent le message en attendant un admin), blocage personnel, parole coupée.
 *
 * Le « camp » d'un joueur (ce qu'il a joué sur le match du message) ne s'affiche
 * qu'une fois le match commencé : avant, on copierait les bons joueurs, même
 * règle que les profils.
 */
@Service
public class NightThreadService {

    static final int MAX_LENGTH = 280;
    static final Duration MIN_INTERVAL = Duration.ofSeconds(3);
    static final int MAX_PER_HOUR = 120;
    static final int REPORTS_TO_HIDE = 3;
    private static final LocalTime NIGHT_START = LocalTime.NOON;
    // Lien écrit en clair ou nom de domaine (« monsite.com », « bit.ly/x »).
    private static final Pattern LINK = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[\\p{L}\\p{N}-]+\\.(com|net|org|ci|fr|io|me|gg|ly|co|tv|app|xyz|info|bet)\\b)");

    public record Message(String id, boolean auto, String username, Integer avatarNumber, String avatarPosition,
                          String avatarColorway, String avatarIcon, String text, String matchId, String matchLabel,
                          String camp, Instant createdAt, Map<String, Integer> reactionCounts, List<String> myReactions,
                          boolean mine, boolean reportedByMe) {}

    /** `cannotWrite` : pourquoi ce joueur ne peut pas écrire, null s'il le peut. */
    public record Feed(LocalDate night, List<Message> messages, String cannotWrite) {}

    public record Blocked(String username) {}

    private final NightMessageRepository messageRepository;
    private final MessageReportRepository reportRepository;
    private final UserBlockRepository blockRepository;
    private final ChatWordRepository wordRepository;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final BetSelectionRepository selectionRepository;
    private final MatchRepository matchRepository;
    private final ActivityReactionRepository reactionRepository;
    private final SocialNotifier socialNotifier;
    private final Clock clock;

    @Autowired
    public NightThreadService(NightMessageRepository messageRepository, MessageReportRepository reportRepository,
                              UserBlockRepository blockRepository, ChatWordRepository wordRepository, UserRepository userRepository,
                              BetRepository betRepository, BetSelectionRepository selectionRepository, MatchRepository matchRepository,
                              ActivityReactionRepository reactionRepository, SocialNotifier socialNotifier) {
        this(messageRepository, reportRepository, blockRepository, wordRepository, userRepository, betRepository,
                selectionRepository, matchRepository, reactionRepository, socialNotifier, Clock.system(BankrollService.ZONE));
    }

    NightThreadService(NightMessageRepository messageRepository, MessageReportRepository reportRepository,
                       UserBlockRepository blockRepository, ChatWordRepository wordRepository, UserRepository userRepository,
                       BetRepository betRepository, BetSelectionRepository selectionRepository, MatchRepository matchRepository,
                       ActivityReactionRepository reactionRepository, SocialNotifier socialNotifier, Clock clock) {
        this.messageRepository = messageRepository;
        this.reportRepository = reportRepository;
        this.blockRepository = blockRepository;
        this.wordRepository = wordRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.selectionRepository = selectionRepository;
        this.matchRepository = matchRepository;
        this.reactionRepository = reactionRepository;
        this.socialNotifier = socialNotifier;
        this.clock = clock;
    }

    /** La soirée NBA de cet instant : 12h GMT → 12h GMT le lendemain, comme la cote boostée. */
    public static LocalDate nightOf(Instant at) {
        ZonedDateTime t = at.atZone(BankrollService.ZONE);
        return t.toLocalTime().isBefore(NIGHT_START) ? t.toLocalDate().minusDays(1) : t.toLocalDate();
    }

    @Transactional(readOnly = true)
    public Feed feed(String userId, String matchId) {
        Instant now = clock.instant();
        LocalDate night = nightOf(now);
        Set<String> blocked = blockRepository.findByBlockerId(userId).stream().map(UserBlock::getBlockedId).collect(Collectors.toSet());
        List<NightMessage> raw = (matchId != null
                ? messageRepository.findTop100ByMatchIdOrderByCreatedAtDesc(matchId)
                : messageRepository.findTop100ByNightOrderByCreatedAtDesc(night)).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getHidden()))
                .filter(m -> m.getUser() == null || !blocked.contains(m.getUser().getId()))
                .toList();

        Set<String> matchIds = raw.stream().map(NightMessage::getMatchId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Match> matches = matchRepository.findAllById(matchIds).stream().collect(Collectors.toMap(Match::getId, Function.identity()));
        Map<String, String> camps = camps(raw, matches, now);

        List<String> ids = raw.stream().map(NightMessage::getId).toList();
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<String>> mine = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ActivityReaction r : reactionRepository.findByTargetTypeInAndTargetIdIn(List.of("NIGHT"), ids)) {
                counts.computeIfAbsent(r.getTargetId(), k -> new HashMap<>()).merge(r.getEmoji(), 1, Integer::sum);
                if (r.getUser().getId().equals(userId)) mine.computeIfAbsent(r.getTargetId(), k -> new ArrayList<>()).add(r.getEmoji());
            }
        }
        Set<String> reported = ids.isEmpty() ? Set.of() : reportRepository.findByMessageIdIn(ids).stream()
                .filter(r -> userId.equals(r.getReporterId())).map(MessageReport::getMessageId).collect(Collectors.toSet());

        List<Message> messages = raw.stream().map(m -> {
            User u = m.getUser();
            Match match = m.getMatchId() == null ? null : matches.get(m.getMatchId());
            return new Message(m.getId(), u == null, u == null ? null : u.getUsername(),
                    u == null ? null : u.getAvatarNumber(), u == null ? null : u.getAvatarPosition(),
                    u == null ? null : u.getAvatarColorway(), u == null ? null : u.getAvatarIcon(),
                    m.getText(), m.getMatchId(), match == null ? null : label(match),
                    u == null ? null : camps.get(campKey(u.getId(), m.getMatchId())), m.getCreatedAt(),
                    counts.getOrDefault(m.getId(), Map.of()), mine.getOrDefault(m.getId(), List.of()),
                    u != null && u.getId().equals(userId), reported.contains(m.getId()));
        }).toList();

        String cannotWrite = cannotWrite(userId, now);
        if (cannotWrite == null && matchId != null && !isTonight(matchId, night)) cannotWrite = "Le fil de ce match n'est pas ouvert.";
        return new Feed(night, messages, cannotWrite);
    }

    // On écrit dans le fil de la soirée en cours : pas sur le match d'une autre soirée.
    private boolean isTonight(String matchId, LocalDate night) {
        return matchRepository.findById(matchId)
                .filter(m -> m.getDate() != null && nightOf(m.getDate()).equals(night)).isPresent();
    }

    private static String label(Match m) {
        return m.getAwayTeam().getAbbreviation() + " @ " + m.getHomeTeam().getAbbreviation();
    }

    private static String campKey(String userId, String matchId) {
        return userId + "|" + matchId;
    }

    // Ce que chaque auteur a joué sur le match de son message, seulement si ce match a commencé.
    private Map<String, String> camps(List<NightMessage> raw, Map<String, Match> matches, Instant now) {
        Set<String> started = matches.values().stream()
                .filter(m -> m.getStatus() != MatchStatus.SCHEDULED || (m.getDate() != null && !m.getDate().isAfter(now)))
                .map(Match::getId).collect(Collectors.toSet());
        Map<String, String> camps = new HashMap<>();
        for (String matchId : started) {
            Set<String> authors = raw.stream().filter(m -> matchId.equals(m.getMatchId()) && m.getUser() != null)
                    .map(m -> m.getUser().getId()).collect(Collectors.toSet());
            if (authors.isEmpty()) continue;
            for (BetSelection s : selectionRepository.findFriendsOnMatch(matchId, authors)) {
                camps.merge(campKey(s.getBet().getUser().getId(), matchId), s.getLabel(), (a, b) -> a);
            }
        }
        return camps;
    }

    private String cannotWrite(String userId, Instant now) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "Compte introuvable.";
        if (user.getChatMutedUntil() != null && user.getChatMutedUntil().isAfter(now)) {
            return "Tu ne peux plus écrire dans le fil pour le moment.";
        }
        if (betRepository.findFirstByUser_Id(userId).isEmpty()) return "Pose ton premier ticket pour écrire dans le fil.";
        return null;
    }

    @Transactional
    public void post(String userId, String rawText, String matchId) {
        Instant now = clock.instant();
        String refused = cannotWrite(userId, now);
        if (refused != null) throw new IllegalArgumentException(refused);

        String text = rawText == null ? "" : rawText.strip();
        if (text.isEmpty()) throw new IllegalArgumentException("Message vide.");
        if (text.length() > MAX_LENGTH) throw new IllegalArgumentException("Message trop long (" + MAX_LENGTH + " caractères au plus).");
        if (LINK.matcher(text).find()) throw new IllegalArgumentException("Les liens ne sont pas autorisés dans le fil.");
        if (containsBannedWord(text, wordRepository.findAll().stream().map(ChatWord::getWord).toList())) {
            throw new IllegalArgumentException("Ce message contient un mot refusé.");
        }
        Optional<NightMessage> last = messageRepository.findTopByUser_IdOrderByCreatedAtDesc(userId);
        if (last.isPresent() && Duration.between(last.get().getCreatedAt(), now).compareTo(MIN_INTERVAL) < 0) {
            throw new IllegalArgumentException("Doucement : un message toutes les 3 secondes.");
        }
        if (last.isPresent() && text.equalsIgnoreCase(last.get().getText())) {
            throw new IllegalArgumentException("Tu viens déjà d'envoyer ce message.");
        }
        if (messageRepository.countByUser_IdAndCreatedAtAfter(userId, now.minus(Duration.ofHours(1))) >= MAX_PER_HOUR) {
            throw new IllegalArgumentException("Beaucoup de messages en peu de temps : réessaie dans un moment.");
        }
        if (matchId != null && !isTonight(matchId, nightOf(now))) throw new IllegalArgumentException("Le fil de ce match n'est pas ouvert.");

        NightMessage message = new NightMessage();
        message.setId(UUID.randomUUID().toString());
        message.setNight(nightOf(now));
        message.setMatchId(matchId);
        message.setUser(userRepository.findById(userId).orElseThrow());
        message.setText(text);
        message.setCreatedAt(now);
        messageRepository.save(message);
        socialNotifier.mentions(message.getUser(), text, matchId == null ? "/soiree" : "/matches/" + matchId + "?onglet=fil", null);
    }

    /**
     * Mots entiers (pluriel compris), sans tenir compte des accents ni des majuscules :
     * « con » ne bloque pas « contre ». Lu deux fois : la ponctuation sépare les mots
     * (« de con,toi ») mais peut aussi servir à les cacher (« p.u.t.e »).
     */
    static boolean containsBannedWord(String text, List<String> words) {
        String ascii = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        String spaced = " " + ascii.replaceAll("[^a-z]+", " ").trim() + " ";
        String joined = " " + PlayerPropsService.normalize(text) + " ";
        return words.stream().map(PlayerPropsService::normalize).filter(w -> !w.isBlank()).anyMatch(w ->
                spaced.contains(" " + w + " ") || spaced.contains(" " + w + "s ")
                        || joined.contains(" " + w + " ") || joined.contains(" " + w + "s "));
    }

    @Transactional
    public void delete(String userId, String messageId) {
        NightMessage m = messageRepository.findById(messageId).orElseThrow(() -> new IllegalArgumentException("Message introuvable."));
        if (m.getUser() == null || !m.getUser().getId().equals(userId)) throw new IllegalStateException("Ce message n'est pas le tien.");
        remove(m);
    }

    void remove(NightMessage m) {
        reportRepository.deleteAllForMessage(m.getId());
        reactionRepository.deleteAll(reactionRepository.findByTargetTypeInAndTargetIdIn(List.of("NIGHT"), List.of(m.getId())));
        messageRepository.delete(m);
    }

    /** Un signalement par joueur et par message ; un message rétabli par un admin ne se masque plus. */
    @Transactional
    public void report(String userId, String messageId) {
        NightMessage m = messageRepository.findById(messageId).orElseThrow(() -> new IllegalArgumentException("Message introuvable."));
        if (m.getUser() == null || m.getUser().getId().equals(userId)) throw new IllegalArgumentException("Tu ne peux pas signaler ce message.");
        if (Boolean.TRUE.equals(m.getReviewed())) return;
        String id = userId + ":" + messageId;
        if (reportRepository.existsById(id)) return;
        MessageReport report = new MessageReport();
        report.setId(id);
        report.setMessageId(messageId);
        report.setReporterId(userId);
        report.setCreatedAt(clock.instant());
        reportRepository.saveAndFlush(report);
        if (reportRepository.countByMessageId(messageId) >= REPORTS_TO_HIDE) m.setHidden(true);
    }

    @Transactional
    public void block(String userId, String username) {
        User other = userRepository.findByUsernameIgnoreCase(username == null ? "" : username).orElseThrow(() -> new IllegalArgumentException("Joueur introuvable."));
        if (other.getId().equals(userId)) throw new IllegalArgumentException("Tu ne peux pas te bloquer toi-même.");
        UserBlock block = new UserBlock();
        block.setId(userId + ":" + other.getId());
        block.setBlockerId(userId);
        block.setBlockedId(other.getId());
        blockRepository.save(block);
    }

    @Transactional
    public void unblock(String userId, String username) {
        userRepository.findByUsernameIgnoreCase(username == null ? "" : username)
                .ifPresent(other -> blockRepository.deleteById(userId + ":" + other.getId()));
    }

    @Transactional(readOnly = true)
    public List<Blocked> blocks(String userId) {
        List<String> ids = blockRepository.findByBlockerId(userId).stream().map(UserBlock::getBlockedId).toList();
        return userRepository.findAllById(ids).stream().map(u -> new Blocked(u.getUsername())).toList();
    }
}
