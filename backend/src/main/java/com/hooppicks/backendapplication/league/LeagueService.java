package com.hooppicks.backendapplication.league;


import com.hooppicks.backendapplication.push.PushService;
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
    private final PushService pushService;

    public LeagueService(LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                          UserRepository userRepository, BetRepository betRepository,
                          NotificationRepository notificationRepository,
                          ActivityReactionRepository activityReactionRepository, PushService pushService) {
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.notificationRepository = notificationRepository;
        this.activityReactionRepository = activityReactionRepository;
        this.pushService = pushService;
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
            int avatarNumber, String avatarPosition, String avatarColorway, String avatarIcon,
            boolean deletable, List<LeagueActivityDto.CopySelection> selections, List<String> mentions
    ) {
        RawActivity(String targetType, String targetId, User user, String message, java.time.Instant occurredAt) {
            this(targetType, targetId, user.getUsername(), message, occurredAt, user.getAvatarNumber(),
                    user.getAvatarPosition(), user.getAvatarColorway(), user.getAvatarIcon(), false, List.of(), null);
        }
    }

    // Facultatifs : les tests construisent ce service sans eux.
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.hooppicks.backendapplication.repository.LeagueMessageRepository messageRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.hooppicks.backendapplication.repository.WeeklyTitleRepository titleRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.hooppicks.backendapplication.repository.MatchRepository matchRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.hooppicks.backendapplication.social.SocialNotifier socialNotifier;

    static final int MAX_MESSAGE_LENGTH = 280;
    // Garde-fou anti-spam : largement au-dessus d'une discussion normale.
    static final int MAX_MESSAGES_PER_HOUR = 30;
    private static final int FEED_SIZE = 30;

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
            activity.add(new RawActivity("MEMBERSHIP", m.getId(), m.getUser(), "a rejoint la ligue", m.getJoinedAt()));
        }

        for (Bet bet : betRepository.findTop10ByUser_IdInAndStatusOrderByPlacedAtDesc(memberIds, BetStatus.PENDING)) {
            User u = bet.getUser();
            activity.add(new RawActivity(
                    "BET", bet.getId(), u.getUsername(),
                    "a misé " + bet.getStake() + " pts sur " + selectionsSummary(bet), bet.getPlacedAt(),
                    u.getAvatarNumber(), u.getAvatarPosition(), u.getAvatarColorway(), u.getAvatarIcon(),
                    false, u.getId().equals(requestingUserId) ? List.of() : copyable(bet), null
            ));
        }

        for (Bet bet : betRepository.findTop10ByUser_IdInAndStatusOrderByResolvedAtDesc(memberIds, BetStatus.WON)) {
            // resolvedAt peut être absent sur d'anciens paris résolus par une
            // voie qui ne le renseignait pas (ex. correction manuelle d'un
            // match côté admin, avant que ce champ soit systématique) : repli
            // sur placedAt plutôt que planter le tri juste en dessous.
            java.time.Instant occurredAt = bet.getResolvedAt() != null ? bet.getResolvedAt() : bet.getPlacedAt();
            activity.add(new RawActivity("BET", bet.getId(), bet.getUser(),
                    // Bénéfice net, comme au classement (pas le gain brut, mise comprise).
                    "a gagné un ticket : +" + (bet.getPotentialPayout() - bet.getStake()) + " pts", occurredAt));
        }

        if (titleRepository != null) {
            Map<String, User> members = new HashMap<>();
            memberships.forEach(m -> members.put(m.getUser().getId(), m.getUser()));
            for (com.hooppicks.backendapplication.entity.WeeklyTitle t : titleRepository.findTop5ByLeagueIdOrderByWeekDesc(leagueId)) {
                User champion = members.get(t.getUserId());
                if (champion == null) continue; // parti depuis : le titre reste sur son profil
                activity.add(new RawActivity("TITLE", t.getId(), champion,
                        "est champion de la semaine avec " + t.getPoints() + " pts",
                        com.hooppicks.backendapplication.bankroll.BankrollService.startOf(t.getWeek().plusWeeks(1))));
            }
        }

        if (messageRepository != null) {
            boolean owner = leagueRepository.findById(leagueId).map(l -> requestingUserId.equals(l.getOwnerId())).orElse(false);
            for (com.hooppicks.backendapplication.entity.LeagueMessage m : messageRepository.findTop30ByLeagueIdOrderByCreatedAtDesc(leagueId)) {
                User u = m.getUser();
                activity.add(new RawActivity("MESSAGE", m.getId(), u.getUsername(), m.getText(), m.getCreatedAt(),
                        u.getAvatarNumber(), u.getAvatarPosition(), u.getAvatarColorway(), u.getAvatarIcon(),
                        owner || u.getId().equals(requestingUserId), List.of(),
                        com.hooppicks.backendapplication.chat.NightThreadService.mentionList(m.getMentions())));
            }
        }

        // nullsLast en filet de sécurité supplémentaire : mieux vaut pousser
        // un item mal daté en fin de liste que faire planter tout le fil.
        // Le naturalOrder() est inversé AVANT nullsLast (pas de .reversed()
        // après) : sinon nullsLast se retrouverait inversé lui aussi et les
        // nulls remonteraient en tête du tri au lieu de rester en dernier.
        List<RawActivity> feed = activity.stream()
                .sorted(Comparator.comparing(RawActivity::occurredAt,
                        Comparator.nullsLast(Comparator.<java.time.Instant>naturalOrder().reversed())))
                .limit(FEED_SIZE)
                .toList();

        return attachReactions(feed, requestingUserId);
    }

    /**
     * Sélections d'un ticket en attente que les autres membres peuvent
     * reprendre (« Je joue pareil ») : vide dès qu'un de ses matchs a commencé.
     * placeBet revérifie tout et reprend ses propres cotes.
     */
    private List<LeagueActivityDto.CopySelection> copyable(Bet bet) {
        if (matchRepository == null) return List.of();
        java.time.Instant now = java.time.Instant.now();
        List<String> ids = bet.getSelections().stream().map(BetSelection::getMatchId).toList();
        List<com.hooppicks.backendapplication.entity.Match> matches = matchRepository.findAllById(ids);
        boolean open = matches.size() == ids.size() && matches.stream().allMatch(m ->
                m.getStatus() == com.hooppicks.backendapplication.entity.MatchStatus.SCHEDULED
                        && m.getDate() != null && m.getDate().isAfter(now));
        if (!open) return List.of();
        return bet.getSelections().stream().map(s -> new LeagueActivityDto.CopySelection(s.getMatchId(), s.getMatchLabel(),
                s.getMarket(), s.getOutcome(), s.getLabel(), s.getOdds(), s.getPlayerId(), s.getPropLine())).toList();
    }

    /** Pas de notification : un message par personne à chaque phrase serait vite coupé par tout le monde. */
    @Transactional
    public void postMessage(String leagueId, String userId, String rawText) {
        postMessage(leagueId, userId, rawText, null);
    }

    @Transactional
    public void postMessage(String leagueId, String userId, String rawText, String replyToId) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, userId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }
        String text = rawText == null ? "" : rawText.strip();
        if (text.isEmpty()) throw new IllegalArgumentException("Message vide.");
        if (text.length() > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException("Message trop long (" + MAX_MESSAGE_LENGTH + " caractères au plus).");
        java.time.Instant now = java.time.Instant.now();
        if (messageRepository.countByUser_IdAndCreatedAtAfter(userId, now.minus(java.time.Duration.ofHours(1))) >= MAX_MESSAGES_PER_HOUR) {
            throw new IllegalArgumentException("Beaucoup de messages en peu de temps : réessaie dans un moment.");
        }
        com.hooppicks.backendapplication.entity.LeagueMessage parent = replyToId == null ? null
                : messageRepository.findById(replyToId).filter(m -> leagueId.equals(m.getLeagueId()))
                        .orElseThrow(() -> new IllegalArgumentException("Ce message n'est plus disponible."));
        com.hooppicks.backendapplication.entity.LeagueMessage message = new com.hooppicks.backendapplication.entity.LeagueMessage();
        message.setLeagueId(leagueId);
        if (parent != null) message.setReplyToId(parent.getId());
        message.setUser(userRepository.findById(userId).orElseThrow());
        message.setText(text);
        message.setCreatedAt(now);
        if (socialNotifier != null) {
            Set<String> members = membershipRepository.findByLeagueId(leagueId).stream()
                    .map(m -> m.getUser().getId()).collect(Collectors.toSet());
            String link = "/leagues/" + leagueId;
            List<String> tagged;
            if (parent == null) {
                tagged = socialNotifier.mentions(message.getUser(), text, link, members);
            } else {
                socialNotifier.replied(message.getUser(), parent.getUser(), text, link);
                tagged = socialNotifier.mentions(message.getUser(), text, link, members, parent.getUser().getId());
            }
            message.setMentions(com.hooppicks.backendapplication.chat.NightThreadService.joinMentions(tagged));
        }
        messageRepository.save(message);
    }

    /** Par son auteur ou par le créateur de la ligue, seule modération d'un fil privé entre amis. */
    @Transactional
    public void deleteMessage(String leagueId, String userId, String messageId) {
        com.hooppicks.backendapplication.entity.LeagueMessage message = messageRepository.findById(messageId)
                .filter(m -> leagueId.equals(m.getLeagueId()))
                .orElseThrow(() -> new IllegalArgumentException("Message introuvable."));
        boolean owner = leagueRepository.findById(leagueId).map(l -> userId.equals(l.getOwnerId())).orElse(false);
        if (!owner && !message.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Ce message n'est pas le tien.");
        }
        activityReactionRepository.deleteAll(activityReactionRepository.findByTargetTypeInAndTargetIdIn(List.of("MESSAGE"), List.of(messageId)));
        messageRepository.delete(message);
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
        Map<String, com.hooppicks.backendapplication.dto.ReplyPreview> replies = replies(items);

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
                    counts, mine, item.deletable(), item.selections(), replies.get(item.targetId()), item.mentions()
            );
        }).toList();
    }

    // Lu à l'affichage, jamais copié : un message supprimé ne survit pas dans ses réponses.
    private Map<String, com.hooppicks.backendapplication.dto.ReplyPreview> replies(List<RawActivity> items) {
        if (messageRepository == null) return Map.of();
        List<String> messageIds = items.stream().filter(i -> "MESSAGE".equals(i.targetType())).map(RawActivity::targetId).toList();
        if (messageIds.isEmpty()) return Map.of();
        Map<String, String> parentOf = new HashMap<>();
        for (com.hooppicks.backendapplication.entity.LeagueMessage m : messageRepository.findAllById(messageIds)) {
            if (m.getReplyToId() != null) parentOf.put(m.getId(), m.getReplyToId());
        }
        if (parentOf.isEmpty()) return Map.of();
        Map<String, com.hooppicks.backendapplication.entity.LeagueMessage> parents = messageRepository.findAllById(new java.util.HashSet<>(parentOf.values())).stream()
                .collect(Collectors.toMap(com.hooppicks.backendapplication.entity.LeagueMessage::getId, m -> m));
        Map<String, com.hooppicks.backendapplication.dto.ReplyPreview> replies = new HashMap<>();
        parentOf.forEach((id, parentId) -> {
            com.hooppicks.backendapplication.entity.LeagueMessage parent = parents.get(parentId);
            replies.put(id, parent == null ? com.hooppicks.backendapplication.dto.ReplyPreview.gone(parentId)
                    : com.hooppicks.backendapplication.dto.ReplyPreview.of(parentId, parent.getUser().getUsername(), parent.getText()));
        });
        return replies;
    }

    /**
     * Une seule réaction par membre et par item, comme un vote : réagir avec
     * un autre emoji remplace la réaction précédente, réagir avec le même la
     * retire. (Avant, chaque emoji se basculait indépendamment et on pouvait
     * mettre 👍 et 👎 en même temps, ce qui n'a pas de sens.)
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
        applyReaction(targetType, targetId, requestingUserId, emoji);
    }

    /** Sans contrôle d'appartenance : à l'appelant de vérifier que ce joueur peut réagir à cette cible. */
    @Transactional
    public void applyReaction(String targetType, String targetId, String requestingUserId, String emoji) {
        if (!ALLOWED_EMOJIS.contains(emoji)) {
            throw new IllegalArgumentException("Emoji non supporté.");
        }
        if (!Set.of("BET", "MEMBERSHIP", "MESSAGE", "TITLE", "NIGHT").contains(targetType)) {
            throw new IllegalArgumentException("Type de cible invalide.");
        }

        List<ActivityReaction> mine = activityReactionRepository
                .findByTargetTypeAndTargetIdAndUser_Id(targetType, targetId, requestingUserId);
        boolean sameEmoji = mine.stream().anyMatch(r -> emoji.equals(r.getEmoji()));

        // Toutes supprimées dans les deux cas : retire la réaction (même emoji)
        // ou libère la place pour la nouvelle, et nettoie au passage les
        // doublons créés avant cette règle.
        activityReactionRepository.deleteAll(mine);

        if (!sameEmoji) {
            ActivityReaction reaction = new ActivityReaction();
            reaction.setTargetType(targetType);
            reaction.setTargetId(targetId);
            reaction.setUser(userRepository.findById(requestingUserId).orElseThrow());
            reaction.setEmoji(emoji);
            activityReactionRepository.save(reaction);
            if (socialNotifier != null) socialNotifier.reacted(reaction.getUser(), targetType, targetId, emoji);
        }
    }

    /**
     * Classement scopé aux membres de la ligue. Lève IllegalStateException si
     * l'appelant n'est pas membre : au contrôleur de traduire ça en 403.
     */
    public List<LeaderboardEntryDto> getLeagueLeaderboard(String leagueId, String requestingUserId) {
        return getLeagueLeaderboard(leagueId, requestingUserId, com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod.SEASON);
    }

    /**
     * Même classement, sur une période : la semaine relance la course entre
     * amis chaque lundi, quand le classement saison s'est figé.
     */
    public List<LeaderboardEntryDto> getLeagueLeaderboard(String leagueId, String requestingUserId,
                                                          com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod period) {
        if (membershipRepository.findByLeagueIdAndUserId(leagueId, requestingUserId).isEmpty()) {
            throw new IllegalStateException("Tu n'es pas membre de cette ligue.");
        }

        List<String> memberIds = membershipRepository.findByLeagueId(leagueId).stream()
                .map(m -> m.getUser().getId())
                .toList();

        java.time.Instant since = period.start(java.time.ZonedDateTime.now(com.hooppicks.backendapplication.bankroll.BankrollService.ZONE));
        return LeaderboardEntryDto.fromRows(betRepository.getLeaderboardRawForUsersSince(since, period.phase(), memberIds));
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
                if (messageRepository != null) messageRepository.deleteAllForLeague(leagueId);
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
            pushService.sendToUser(membership.getUser().getId(), new PushService.PushMessage(
                    "Ta ligue s'agrandit", notification.getMessage(), "/leagues/" + league.getId()));
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
