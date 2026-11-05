package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.ChatWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatWordRepository extends JpaRepository<ChatWord, String> {
}
