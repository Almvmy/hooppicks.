package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.entity.LeagueMembership;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.LeagueMembershipRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Les pseudos proposés pendant qu'on écrit @ka : une faute de frappe et personne
 * n'était prévenu. Rien de plus que ce que le classement montre déjà : pseudo et
 * maillot. Dans une ligue, ses membres seulement, comme la notification elle-même.
 */
@RestController
public class MentionController {

    private static final int MAX_RESULTS = 6;

    public record Candidate(String username, int avatarNumber, String avatarPosition, String avatarColorway, String avatarIcon) {}

    private final UserRepository userRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final SessionStore sessionStore;

    public MentionController(UserRepository userRepository, LeagueMembershipRepository membershipRepository, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/mentions")
    public ResponseEntity<?> mentions(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) String leagueId, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        String prefix = q.strip();

        List<User> found;
        if (leagueId != null) {
            if (membershipRepository.findByLeagueIdAndUserId(leagueId, userId).isEmpty()) return ResponseEntity.status(403).build();
            String lower = prefix.toLowerCase(Locale.ROOT);
            found = membershipRepository.findByLeagueId(leagueId).stream().map(LeagueMembership::getUser)
                    .filter(u -> u.getUsername().toLowerCase(Locale.ROOT).startsWith(lower)).toList();
        } else {
            // Sans une première lettre, le fil propose déjà ceux qui y écrivent : pas toute la base.
            found = prefix.isEmpty() ? List.of() : userRepository.findTop7ByUsernameStartingWithIgnoreCaseOrderByUsernameAsc(prefix);
        }
        return ResponseEntity.ok(found.stream()
                .filter(u -> !u.getId().equals(userId))
                .limit(MAX_RESULTS)
                .map(u -> new Candidate(u.getUsername(), u.getAvatarNumber(), u.getAvatarPosition(), u.getAvatarColorway(), u.getAvatarIcon()))
                .toList());
    }
}
