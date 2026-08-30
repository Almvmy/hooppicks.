package com.hooppicks.backendapplication.league;

import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.dto.LeagueActivityDto;
import com.hooppicks.backendapplication.dto.LeagueDto;
import com.hooppicks.backendapplication.dto.LeagueMemberDto;
import com.hooppicks.backendapplication.dto.LeaguePreviewDto;
import com.hooppicks.backendapplication.entity.ActivityReaction;
import com.hooppicks.backendapplication.entity.AppNotification;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.League;
import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.NotificationType;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.ActivityReactionRepository;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.LeagueRepository;
import com.hooppicks.backendapplication.repository.NotificationRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeagueService {

    // Alphabet réduit : sans 0/O/1/I/L pour éviter les confusions à la lecture/saisie.
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Set fixe plutôt qu'un sélecteur d'emoji libre : pas de modération à
    // gérer, et un petit vocabulaire partagé donne un signal plus lisible
    // qu'une infinité de réactions différentes sur le même item.
    private static final Set<String> ALLOWED_EMOJIS = Set.of("👍", "🔥", "👎");

    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityReactionRepository activityReactionRepository;

    public LeagueService(LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                          UserRepository userRepository, BetRepository betRepository,
                          NotificationRepository notificationRepository,
                          ActivityReactionRepository activityReactionRepository) {
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.notificationRepository = notificationRepository;
        this.activityReactionRepository = activityReactionRepository;
    }

    @Transactional
    public LeagueDto createLeague(String ownerId, String name) {
        League league = new League();
        league.setName(name);
        league.setOwnerId(ownerId);
        league.setInviteCode(generateUniqueInviteCode());
        leagueRepository.save(league);

        addMembership(league, ownerId);

        return toDto(league, ownerId);
    }

    @Transactional
    public LeagueDto joinLeague(String userId, String inviteCode) {
        League league = leagueRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Code d'invitation invalide."));

        if (membershipRepository.findByLeagueIdAndUserId(league.getId(), userId).isEmpty()) {
            User joiningUser = addMembership(league, userId);
            notifyExistingMembersOfNewJoin(league, joiningUser);
        }

        return toDto(league, userId);
    }

    public List<LeagueDto> getUserLeagues(String userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(m -> toDto(m.getLeague(), userId))
                .toList();
    }

    /**
     * Aperçu d'une ligue par son code, sans rejoindre : sert à afficher une
     * confirmation ("Rejoindre [nom] ?") avant l'adhésion effective.
     */
    public LeaguePreviewDto previewByCode(String inviteCode) {
        League league = leagueRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Code d'invitation invalide."));
        long memberCount = membershipRepository.countByLeagueId(league.getId());
        return new LeaguePreviewDto(league.getId(), league.getName(), memberCount);
    }

    /**
     * Liste des membres d'une ligue (tous, y compris ceux sans pari résolu :
     * contrairement au classement, qui ne montre que les membres avec au
     * moins un pari WON/LOST). Lève IllegalStateException si l'appelant
     * n'est pas membre, au contrôleur de traduire ça en 403.
     */
    public List<LeagueMemberDto> getMembers(String leagueId, String requestingUserId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        League league = leagueRepository.findById(leagueId).orElseThrow();
        return membershipRepository.findByLeagueId(leagueId).stream()
                .sorted((a, b) -> a.getJoinedAt().compareTo(b.getJoinedAt()))
                .map(m -> new LeagueMemberDto(
                        m.getUser().getUsername(),
                        m.getUser().getId().equals(league.getOwnerId()),
                        m.getJoinedAt(),
                        m.getUser().getAvatarNumber(),
                        m.getUser().getAvatarPosition(),
                        m.getUser().getAvatarColorway(),
                        m.getUser().getAvatarIcon()
                ))
                .toList();
    }

    private record RawActivity(
            String targetType, String targetId, String username, String message, java.time.Instant occurredAt,
            int avatarNumber, String avatarPosition, String avatarColorway, String avatarIcon
    ) {}

    /**
     * Fil d'activité : adhésions + paris en cours + gros paris gagnés
     * récemment parmi les membres, fusionnés et triés du plus récent au
     * plus ancien. Les paris PENDING (pas juste WON) rendent la ligue vivante
     * en direct : voir ce que les autres viennent de miser, pas seulement le
     * résultat une fois le match fini. Même garde d'appartenance que le reste
     * (403 via IllegalStateException).
     */
    public List<LeagueActivityDto> getRecentActivity(String leagueId, String requestingUserId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        List<LeagueMembership> memberships = membershipRepository.findByLeagueId(leagueId);
        List<String> memberIds = memberships.stream().map(m -> m.getUser().getId()).toList();

        List<RawActivity> activity = new ArrayList<>();

        for (LeagueMembership m : memberships) {
            activity.add(new RawActivity(
                    "MEMBERSHIP", m.getId(), m.getUser().getUsername(), "a rejoint la ligue", m.getJoinedAt(),
                    m.getUser().getAvatarNumber(), m.getUser().getAvatarPosition(),
                    m.getUser().getAvatarColorway(), m.getUser().getAvatarIcon()
            ));
        }

        for (Bet bet : betRepository.findTop10ByUser_IdInAndStatusOrderByPlacedAtDesc(memberIds, BetStatus.PENDING)) {
            activity.add(new RawActivity(
                    "BET", bet.getId(), bet.getUser().getUsername(),
                    "a misé " + bet.getStake() + " pts sur " + selectionsSummary(bet), bet.getPlacedAt(),
                    bet.getUser().getAvatarNumber(), bet.getUser().getAvatarPosition(),
                    bet.getUser().getAvatarColorway(), bet.getUser().getAvatarIcon()
            ));
        }

        for (Bet bet : betRepository.findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(memberIds, BetStatus.WON)) {
            // resolvedAt peut être absent sur d'anciens paris résolus par une
            // voie qui ne le renseignait pas (ex. correction manuelle d'un
            // match côté admin, avant que ce champ soit systématique) : repli
            // sur placedAt plutôt que planter le tri juste en dessous.
            java.time.Instant occurredAt = bet.getResolvedAt() != null ? bet.getResolvedAt() : bet.getPlacedAt();
            activity.add(new RawActivity(
                    "BET", bet.getId(), bet.getUser().getUsername(),
                    "a gagné un ticket : +" + bet.getPotentialPayout() + " pts", occurredAt,
                    bet.getUser().getAvatarNumber(), bet.getUser().getAvatarPosition(),
                    bet.getUser().getAvatarColorway(), bet.getUser().getAvatarIcon()
            ));
        }

        // nullsLast en filet de sécurité supplémentaire : mieux vaut pousser
        // un item mal daté en fin de liste que faire planter tout le fil.
        // Le naturalOrder() est inversé AVANT nullsLast (pas de .reversed()
        // après) : sinon nullsLast se retrouverait inversé lui aussi et les
        // nulls remonteraient en tête du tri au lieu de rester en dernier.
        List<RawActivity> top10 = activity.stream()
                .sorted(Comparator.comparing(RawActivity::occurredAt,
                        Comparator.nullsLast(Comparator.<java.time.Instant>naturalOrder().reversed())))
                .limit(10)
                .toList();

        return attachReactions(top10, requestingUserId);
    }

    private String selectionsSummary(Bet bet) {
        return bet.getSelections().stream().map(BetSelection::getLabel).collect(Collectors.joining(" + "));
    }

    /**
     * Ajoute les compteurs de réactions à chaque item du fil, en un seul
     * aller-retour groupé (pas une requête par item) : même approche que
     * PickPercentagesService pour le même genre de raison.
     */
    private List<LeagueActivityDto> attachReactions(List<RawActivity> items, String requestingUserId) {
        if (items.isEmpty()) return List.of();

        List<String> targetTypes = items.stream().map(RawActivity::targetType).distinct().toList();
        List<String> targetIds = items.stream().map(RawActivity::targetId).toList();
        List<ActivityReaction> reactions =
                activityReactionRepository.findByTargetTypeInAndTargetIdIn(targetTypes, targetIds);

        Map<String, List<ActivityReaction>> byTarget = reactions.stream()
                .collect(Collectors.groupingBy(ActivityReaction::getTargetId));

        return items.stream().map(item -> {
            List<ActivityReaction> forTarget = byTarget.getOrDefault(item.targetId(), List.of());
            Map<String, Integer> counts = new HashMap<>();
            List<String> mine = new ArrayList<>();
            for (ActivityReaction r : forTarget) {
                counts.merge(r.getEmoji(), 1, Integer::sum);
                if (r.getUser().getId().equals(requestingUserId)) mine.add(r.getEmoji());
            }
            return new LeagueActivityDto(
                    item.targetType(), item.targetId(), item.username(), item.message(), item.occurredAt(),
                    item.avatarNumber(), item.avatarPosition(), item.avatarColorway(), item.avatarIcon(),
                    counts, mine
            );
        }).toList();
    }

    /**
     * Bascule (ajoute/retire) la réaction de l'appelant sur un item du fil.
     * L'appelant doit être membre de la ligue affichée, pas forcément lié au
     * pari/l'adhésion ciblé : c'est le fil de LA LIGUE qu'on protège, pas
     * l'item individuel (cf. getRecentActivity, même garde).
     */
    @Transactional
    public void toggleReaction(String leagueId, String requestingUserId, String targetType, String targetId,
                                String emoji) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }
        if (!ALLOWED_EMOJIS.contains(emoji)) {
            throw new IllegalArgumentException("Emoji non supporté.");
        }
        if (!"BET".equals(targetType) && !"MEMBERSHIP".equals(targetType)) {
            throw new IllegalArgumentException("Type de cible invalide.");
        }

        var existing = activityReactionRepository
                .findByTargetTypeAndTargetIdAndUser_IdAndEmoji(targetType, targetId, requestingUserId, emoji);

        if (existing.isPresent()) {
            activityReactionRepository.delete(existing.get());
        } else {
            ActivityReaction reaction = new ActivityReaction();
            reaction.setTargetType(targetType);
            reaction.setTargetId(targetId);
            reaction.setUser(userRepository.findById(requestingUserId).orElseThrow());
            reaction.setEmoji(emoji);
            activityReactionRepository.save(reaction);
        }
    }

    /**
     * Classement scopé aux membres de la ligue. Lève IllegalStateException si
     * l'appelant n'est pas membre : au contrôleur de traduire ça en 403.
     */
    public List<LeaderboardEntryDto> getLeagueLeaderboard(String leagueId, String requestingUserId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        List<String> memberIds = membershipRepository.findByLeagueId(leagueId).stream()
                .map(m -> m.getUser().getId())
                .toList();

        List<Object[]> rows = betRepository.getLeaderboardRawForUsers(memberIds);
        List<LeaderboardEntryDto> result = new ArrayList<>();

        int rank = 1;
        for (Object[] row : rows) {
            String username = (String) row[1];
            long points = (Long) row[2];
            long totalBets = (Long) row[3];
            long wonBets = (Long) row[4];
            int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);

            result.add(new LeaderboardEntryDto(rank++, username, (int) points, winRate, (int) totalBets,
                    (Integer) row[5], (String) row[6], (String) row[7], (String) row[8]));
        }
        return result;
    }

    @Transactional
    public void leaveLeague(String leagueId, String userId) {
        membershipRepository.findByLeagueIdAndUserId(leagueId, userId).ifPresent(membership -> {
            League league = membership.getLeague();
            boolean wasOwner = league.getOwnerId().equals(userId);

            membershipRepository.delete(membership);
            membershipRepository.flush();

            List<LeagueMembership> remaining = membershipRepository.findByLeagueId(leagueId);

            // Plus personne dans la ligue : autant la nettoyer plutôt que de
            // laisser un code d'invitation orphelin traîner indéfiniment.
            if (remaining.isEmpty()) {
                leagueRepository.deleteById(leagueId);
            } else if (wasOwner) {
                // Pas de transfert manuel dans ce produit : si le proprio part,
                // la ligue ne doit pas se retrouver avec un ownerId pointant
                // vers quelqu'un qui n'en fait plus partie : on transfère au
                // membre le plus ancien plutôt que de laisser la propriété orpheline.
                LeagueMembership oldest = remaining.stream()
                        .min(Comparator.comparing(LeagueMembership::getJoinedAt))
                        .orElseThrow();
                league.setOwnerId(oldest.getUser().getId());
                leagueRepository.save(league);
            }
        });
    }

    private User addMembership(League league, String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(league);
        membership.setUser(user);
        membershipRepository.save(membership);
        return user;
    }

    private void notifyExistingMembersOfNewJoin(League league, User joiningUser) {
        for (LeagueMembership membership : membershipRepository.findByLeagueId(league.getId())) {
            if (membership.getUser().getId().equals(joiningUser.getId())) continue; // pas de notif à soi-même
            if (!membership.getUser().isNotifyLeagueActivity()) continue;

            AppNotification notification = new AppNotification();
            notification.setUser(membership.getUser());
            notification.setType(NotificationType.SYSTEM);
            notification.setMessage(joiningUser.getUsername() + " a rejoint ta ligue \"" + league.getName() + "\" !");
            notificationRepository.save(notification);
        }
    }

    private LeagueDto toDto(League league, String requestingUserId) {
        long memberCount = membershipRepository.countByLeagueId(league.getId());
        boolean isOwner = league.getOwnerId().equals(requestingUserId);
        return new LeagueDto(league.getId(), league.getName(), league.getInviteCode(),
                memberCount, isOwner, league.getCreatedAt());
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (leagueRepository.existsByInviteCode(code));
        return code;
    }
}
