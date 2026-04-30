package com.arwase.flowberryapp.models;

import java.util.List;

public class InfoArticle {
    public static final String CATEGORY_FLOWBERRY = "flowberry";
    public static final String CATEGORY_CYCLE = "cycle";

    public final String id;
    public final String category;
    public final String title;
    public final String summary;
    public final List<String> paragraphs;
    public final List<ArticleSource> sources;

    public InfoArticle(String id,
                       String category,
                       String title,
                       String summary,
                       List<String> paragraphs,
                       List<ArticleSource> sources) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.paragraphs = paragraphs;
        this.sources = sources;
    }

    public static class ArticleSource {
        public final String label;
        public final String url;

        public ArticleSource(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }
}
