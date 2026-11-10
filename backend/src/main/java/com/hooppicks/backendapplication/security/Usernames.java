package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Les pseudos tels qu'on les enregistre et qu'on les cherche. L'inscription prenait
 * le pseudo tel quel : une espace oubliée à la fin (clavier de téléphone) donnait
 * un joueur introuvable dès qu'on le cherchait par son nom (défi, profil, partage).
 */
public final class Usernames {

    public static final int MIN = 3;
    public static final int MAX = 20;

    // Espaces de toute sorte, y compris insécables et invisibles (copier-coller).
    private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}\\u200B\\u200C\\u200D\\u2060\\uFEFF]+");

    private Usernames() {}

    /** Espaces du début et de la fin retirés, une seule espace entre deux mots. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        return SPACES.matcher(raw).replaceAll(" ").strip();
    }

    /** Null si le pseudo (déjà normalisé) est accepté, sinon le message à afficher. */
    public static String invalidReason(String username) {
        int length = username.codePointCount(0, username.length());
        if (length < MIN || length > MAX) return "Le pseudo doit faire entre " + MIN + " et " + MAX + " caractères.";
        // « @ » sert aux mentions dans les fils, « / » casserait l'adresse du profil.
        if (username.contains("@") || username.contains("/") || username.contains("\\")) return "Le pseudo ne peut pas contenir @, / ni \\.";
        if (username.codePoints().anyMatch(Character::isISOControl)) return "Le pseudo contient un caractère invalide.";
        return null;
    }

    /** Le pseudo tel qu'il est écrit, sinon nettoyé : un compte ancien peut encore avoir son espace en trop. */
    public static Optional<User> find(UserRepository repository, String raw) {
        if (raw == null) return Optional.empty();
        Optional<User> exact = repository.findByUsername(raw);
        if (exact.isPresent()) return exact;
        String clean = normalize(raw);
        return clean.isEmpty() || clean.equals(raw) ? Optional.empty() : repository.findByUsername(clean);
    }
}
