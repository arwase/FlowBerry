package com.arwase.flowberryapp.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.logic.InfoArticleRepository;
import com.arwase.flowberryapp.models.InfoArticle;
import com.google.android.material.appbar.MaterialToolbar;

public class ArticleActivity extends AppCompatActivity {

    public static final String EXTRA_ARTICLE_ID = "article_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        MaterialToolbar topBar = findViewById(R.id.topAppBar);
        TextView textArticleCategory = findViewById(R.id.textArticleCategory);
        TextView textArticleTitle = findViewById(R.id.textArticleTitle);
        TextView textArticleSummary = findViewById(R.id.textArticleSummary);
        LinearLayout layoutArticleBody = findViewById(R.id.layoutArticleBody);
        LinearLayout layoutArticleSources = findViewById(R.id.layoutArticleSources);
        View cardArticleSources = findViewById(R.id.cardArticleSources);

        if (topBar != null) {
            topBar.setNavigationOnClickListener(v -> finish());
        }

        String articleId = getIntent().getStringExtra(EXTRA_ARTICLE_ID);
        InfoArticle article = InfoArticleRepository.getById(this, articleId);
        if (article == null) {
            finish();
            return;
        }

        if (topBar != null) {
            topBar.setTitle(article.title);
        }
        textArticleCategory.setText(getCategoryLabel(article.category));
        textArticleTitle.setText(article.title);
        textArticleSummary.setText(article.summary);

        for (String paragraph : article.paragraphs) {
            TextView bodyText = new TextView(this);
            bodyText.setText(paragraph);
            bodyText.setTextSize(15f);
            bodyText.setTextColor(getColor(R.color.flowberry_text_primary));
            bodyText.setLineSpacing(0f, 1.25f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(12);
            bodyText.setLayoutParams(params);
            layoutArticleBody.addView(bodyText);
        }

        if (article.sources.isEmpty()) {
            cardArticleSources.setVisibility(View.GONE);
        } else {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (InfoArticle.ArticleSource source : article.sources) {
                View sourceCard = inflater.inflate(R.layout.item_article_source, layoutArticleSources, false);
                TextView sourceLabel = sourceCard.findViewById(R.id.textSourceLabel);
                TextView sourceUrl = sourceCard.findViewById(R.id.textSourceUrl);

                sourceLabel.setText(source.label);
                sourceUrl.setText(source.url);

                View.OnClickListener openSourceClick = v -> openSourceUrl(source.url);
                sourceCard.setOnClickListener(openSourceClick);
                sourceUrl.setOnClickListener(openSourceClick);
                layoutArticleSources.addView(sourceCard);
            }
        }
    }

    private String getCategoryLabel(String category) {
        if (InfoArticle.CATEGORY_FLOWBERRY.equals(category)) {
            return getString(R.string.article_category_flowberry);
        }
        return getString(R.string.article_category_cycle);
    }

    private void openSourceUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
