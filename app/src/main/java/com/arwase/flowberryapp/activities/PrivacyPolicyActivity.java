package com.arwase.flowberryapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.arwase.flowberryapp.R;
import com.google.android.material.appbar.MaterialToolbar;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        MaterialToolbar topBar = findViewById(R.id.topAppBar);
        TextView textArticleCategory = findViewById(R.id.textArticleCategory);
        TextView textArticleTitle = findViewById(R.id.textArticleTitle);
        TextView textArticleSummary = findViewById(R.id.textArticleSummary);
        LinearLayout layoutArticleBody = findViewById(R.id.layoutArticleBody);
        View cardArticleSources = findViewById(R.id.cardArticleSources);

        if (topBar != null) {
            topBar.setTitle(R.string.privacy_policy_title);
            topBar.setNavigationOnClickListener(v -> finish());
        }

        textArticleCategory.setText(R.string.privacy_policy_category);
        textArticleTitle.setText(R.string.privacy_policy_title);
        textArticleSummary.setText(R.string.privacy_policy_summary);
        cardArticleSources.setVisibility(View.GONE);

        int[] paragraphs = new int[]{
                R.string.privacy_policy_body_1,
                R.string.privacy_policy_body_2,
                R.string.privacy_policy_body_3,
                R.string.privacy_policy_body_4,
                R.string.privacy_policy_body_5,
                R.string.privacy_policy_body_6,
                R.string.privacy_policy_body_7,
                R.string.privacy_policy_body_8,
                R.string.privacy_policy_body_9,
                R.string.privacy_policy_body_10,
                R.string.privacy_policy_body_11
        };

        for (int i = 0; i < paragraphs.length; i++) {
            int paragraphRes = paragraphs[i];
            TextView bodyText = new TextView(this);
            bodyText.setText(paragraphRes);
            bodyText.setTextSize(15f);
            bodyText.setTextColor(getColor(R.color.flowberry_text_primary));
            bodyText.setLineSpacing(0f, 1.25f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.topMargin = dp(8);
            }
            params.bottomMargin = dp(16);
            bodyText.setLayoutParams(params);
            layoutArticleBody.addView(bodyText);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
