package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Les @pseudo d'un message, retrouvés parmi les vrais comptes. Un simple « @mot » ne
 * suffit pas : la proposition pendant la frappe insère « @Spidey 🕷 », et couper à la
 * première espace cherchait un « Spidey » qui n'existe pas (ni notification, ni lien).
 * On part du premier mot, on regarde les pseudos qui commencent comme lui, et on garde
 * le plus long qui est écrit en entier juste après le @.
 */
public final class Mentions {

    // Pas après une lettre : « moi@gmail.com » n'est pas un tag.
    private static final Pattern AT = Pattern.compile("(?<![\\p{L}\\p{N}_])@");
    private static final Pattern FIRST_WORD = Pattern.compile("^\\S{1,30}");
    private static final Pattern SIMPLE = Pattern.compile("^[\\p{L}\\p{N}_.-]{2,30}");

    private Mentions() {}

    public static List<User> resolve(String text, UserRepository users, int max) {
        List<User> found = new ArrayList<>();
        if (text == null) return found;
        Matcher at = AT.matcher(text);
        while (at.find() && found.size() < max) {
            String rest = text.substring(at.end());
            User longest = longestName(rest, users);
            User user = longest != null ? longest : simpleName(rest, users);
            if (user != null && found.stream().noneMatch(u -> u.getId().equals(user.getId()))) found.add(user);
        }
        return found;
    }

    private static User longestName(String rest, UserRepository users) {
        Matcher word = FIRST_WORD.matcher(rest);
        if (!word.find()) return null;
        // « @Kader, » : la ponctuation collée au mot ne fait pas partie du pseudo cherché.
        String prefix = word.group().replaceAll("[.,;:!?)»\"'-]+$", "");
        if (prefix.isEmpty()) return null;
        User best = null;
        for (User candidate : users.findTop20ByUsernameStartingWithIgnoreCase(prefix)) {
            String name = candidate.getUsername();
            if (name == null || !rest.regionMatches(true, 0, name, 0, name.length()) || !endsWord(rest, name.length())) continue;
            if (best == null || name.length() > best.getUsername().length()) best = candidate;
        }
        return best;
    }

    // Repli : le tag tel qu'on l'écrivait avant, un seul mot (« @Kader. » sans son point).
    private static User simpleName(String rest, UserRepository users) {
        Matcher m = SIMPLE.matcher(rest);
        if (!m.find()) return null;
        String name = m.group().replaceAll("[.-]+$", "");
        return name.length() < 2 ? null : users.findByUsernameIgnoreCase(name).orElse(null);
    }

    private static boolean endsWord(String rest, int index) {
        if (index >= rest.length()) return true;
        int next = rest.codePointAt(index);
        return !Character.isLetterOrDigit(next) && next != '_';
    }
}
