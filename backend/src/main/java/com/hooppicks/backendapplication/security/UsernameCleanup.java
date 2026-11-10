package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pseudos enregistrés avant le nettoyage à l'inscription (espace en trop au début ou
 * à la fin, espaces doublées) : remis au propre une fois, au démarrage. Le joueur ne
 * voit pas la différence, mais son profil et ses défis le retrouvent enfin. Si la
 * version propre est déjà prise par quelqu'un d'autre, on n'y touche pas (noté dans
 * les logs) : `Usernames.find` le retrouve quand même par son nom exact.
 */
@Component
public class UsernameCleanup {

    private static final Logger log = LoggerFactory.getLogger(UsernameCleanup.class);

    private final UserRepository userRepository;

    public UsernameCleanup(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanUp() {
        List<User> users = userRepository.findAll();
        Set<String> taken = new HashSet<>();
        for (User u : users) if (u.getUsername() != null) taken.add(u.getUsername().toLowerCase(Locale.ROOT));

        int fixed = 0;
        for (User u : users) {
            String current = u.getUsername();
            if (current == null) continue;
            String clean = Usernames.normalize(current);
            if (clean.equals(current)) continue;
            if (clean.isEmpty() || taken.contains(clean.toLowerCase(Locale.ROOT))) {
                log.warn("Pseudo du joueur {} laissé tel quel : sa version nettoyée est vide ou déjà prise", u.getId());
                continue;
            }
            taken.remove(current.toLowerCase(Locale.ROOT));
            taken.add(clean.toLowerCase(Locale.ROOT));
            u.setUsername(clean);
            userRepository.save(u);
            fixed++;
        }
        if (fixed > 0) log.info("Pseudos nettoyés (espaces en trop) : {}", fixed);
    }
}
