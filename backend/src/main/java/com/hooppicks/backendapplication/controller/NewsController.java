package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.news.NewsItemDto;
import com.hooppicks.backendapplication.news.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public List<NewsItemDto> getNews() {
        return newsService.fetchLatest();
    }
}
