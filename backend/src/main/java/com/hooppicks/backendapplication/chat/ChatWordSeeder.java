package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.entity.ChatWord;
import com.hooppicks.backendapplication.repository.ChatWordRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Première liste de mots refusés du fil, la suite se tient dans la console.
 * Seulement si la table est vide : un mot retiré à la main ne revient pas.
 *
 * Insultes franches et haine (origine, ethnie, religion, orientation) seulement :
 * « nul », « gaou », « merde », « putain » passent, chambrer fait partie du jeu.
 */
@Component
public class ChatWordSeeder {

    static final List<String> DEFAULT_WORDS = List.of(
            // Insultes
            "con", "conne", "connard", "connarde", "connasse", "conasse", "salope", "salopard", "salaud", "pute",
            "encule", "enculee", "enculer", "enfoire", "enfoiree", "batard", "batarde", "fdp", "ntm", "tg",
            "nique", "niquer", "niquez", "bite", "pouffiasse", "trouduc", "tchoin", "bilakoro", "kahba",
            "fuck", "fucker", "fucking", "motherfucker", "bitch", "asshole", "cunt", "whore", "slut",
            // Orientation
            "pd", "pede", "tapette", "gouine", "tarlouze", "faggot",
            // Origine, ethnie, religion
            "negre", "negresse", "negro", "nigger", "nigga", "bougnoule", "youpin", "bamboula", "macaque",
            "sale noir", "sale blanc", "sale arabe", "sale juif", "sale musulman", "sale chretien",
            "sale dioula", "sale baoule", "sale bete", "sale senoufo", "sale malien", "sale burkinabe", "sale guineen");

    private final ChatWordRepository repository;

    public ChatWordSeeder(ChatWordRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (repository.count() > 0) return;
        repository.saveAll(DEFAULT_WORDS.stream().map(w -> {
            ChatWord word = new ChatWord();
            word.setWord(w);
            return word;
        }).toList());
    }
}
