package com.arwase.flowberryapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.activities.ArticleActivity;
import com.arwase.flowberryapp.logic.InfoArticleRepository;
import com.arwase.flowberryapp.models.InfoArticle;

import java.util.List;

public class InfoFragment extends Fragment {

    public InfoFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        LinearLayout layoutFlowBerryArticles = view.findViewById(R.id.layoutFlowBerryArticles);
        LinearLayout layoutCycleArticles = view.findViewById(R.id.layoutCycleArticles);

        layoutFlowBerryArticles.removeAllViews();
        layoutCycleArticles.removeAllViews();

        bindArticles(inflater, layoutFlowBerryArticles, InfoArticleRepository.getFlowBerryArticles(requireContext()));
        bindArticles(inflater, layoutCycleArticles, InfoArticleRepository.getCycleArticles(requireContext()));

        return view;
    }

    private void bindArticles(LayoutInflater inflater,
                              LinearLayout container,
                              List<InfoArticle> articles) {
        for (InfoArticle article : articles) {
            View card = inflater.inflate(R.layout.item_info_article, container, false);
            TextView title = card.findViewById(R.id.textArticleCardTitle);
            TextView summary = card.findViewById(R.id.textArticleCardSummary);

            title.setText(article.title);
            summary.setText(article.summary);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ArticleActivity.class);
                intent.putExtra(ArticleActivity.EXTRA_ARTICLE_ID, article.id);
                startActivity(intent);
            });

            container.addView(card);
        }
    }
}
