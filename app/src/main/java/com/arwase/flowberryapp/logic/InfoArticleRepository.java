package com.arwase.flowberryapp.logic;

import android.content.Context;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.models.InfoArticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class InfoArticleRepository {

    private InfoArticleRepository() {
    }

    public static List<InfoArticle> getArticles(Context context) {
        return Collections.unmodifiableList(buildArticles(context));
    }

    public static List<InfoArticle> getFlowBerryArticles(Context context) {
        return getArticlesByCategory(context, InfoArticle.CATEGORY_FLOWBERRY);
    }

    public static List<InfoArticle> getCycleArticles(Context context) {
        return getArticlesByCategory(context, InfoArticle.CATEGORY_CYCLE);
    }

    public static List<InfoArticle> getArticlesByCategory(Context context, String category) {
        List<InfoArticle> filtered = new ArrayList<>();
        for (InfoArticle article : getArticles(context)) {
            if (category.equals(article.category)) {
                filtered.add(article);
            }
        }
        return filtered;
    }

    public static InfoArticle getById(Context context, String id) {
        for (InfoArticle article : getArticles(context)) {
            if (article.id.equals(id)) {
                return article;
            }
        }
        return null;
    }

    private static List<InfoArticle> buildArticles(Context context) {
        List<InfoArticle> articles = new ArrayList<>();
        articles.add(buildFlowBerryOverviewArticle(context));
        articles.add(buildAnticipationArticle(context));
        articles.add(buildCycleArticle(context));
        articles.add(buildPhasesArticle(context));
        articles.add(buildPmsArticle(context));
        articles.add(buildPeriodsArticle(context));
        return articles;
    }

    private static InfoArticle buildFlowBerryOverviewArticle(Context context) {
        return new InfoArticle(
                "comment_fonctionne_flowberry",
                InfoArticle.CATEGORY_FLOWBERRY,
                context.getString(R.string.article_flowberry_overview_title),
                context.getString(R.string.article_flowberry_overview_summary),
                strings(context,
                        R.string.article_flowberry_overview_p1,
                        R.string.article_flowberry_overview_p2,
                        R.string.article_flowberry_overview_p3),
                Arrays.asList(
                        new InfoArticle.ArticleSource("NHS - Fertility in the menstrual cycle", "https://www.nhs.uk/conditions/periods/fertility-in-the-menstrual-cycle/"),
                        new InfoArticle.ArticleSource("Mayo Clinic - Menstrual cycle", "https://www.mayoclinic.org/healthy-lifestyle/womens-health/in-depth/menstrual-cycle/art-20047186")
                )
        );
    }

    private static InfoArticle buildAnticipationArticle(Context context) {
        return new InfoArticle(
                "anticipation_flowberry",
                InfoArticle.CATEGORY_FLOWBERRY,
                context.getString(R.string.article_anticipation_title),
                context.getString(R.string.article_anticipation_summary),
                strings(context,
                        R.string.article_anticipation_p1,
                        R.string.article_anticipation_p2,
                        R.string.article_anticipation_p3),
                Arrays.asList(
                        new InfoArticle.ArticleSource("Cleveland Clinic - Ovulation", "https://my.clevelandclinic.org/health/articles/9118-ovulation"),
                        new InfoArticle.ArticleSource("NHS - Periods", "https://www.nhs.uk/conditions/periods/")
                )
        );
    }

    private static InfoArticle buildCycleArticle(Context context) {
        return new InfoArticle(
                "cycle_menstruel",
                InfoArticle.CATEGORY_CYCLE,
                context.getString(R.string.article_cycle_title),
                context.getString(R.string.article_cycle_summary),
                strings(context,
                        R.string.article_cycle_p1,
                        R.string.article_cycle_p2,
                        R.string.article_cycle_p3),
                Arrays.asList(
                        new InfoArticle.ArticleSource("Cleveland Clinic - Menstrual cycle", "https://my.clevelandclinic.org/health/articles/10132-menstrual-cycle"),
                        new InfoArticle.ArticleSource("NHS - Periods", "https://www.nhs.uk/conditions/periods/")
                )
        );
    }

    private static InfoArticle buildPhasesArticle(Context context) {
        return new InfoArticle(
                "phases_cycle",
                InfoArticle.CATEGORY_CYCLE,
                context.getString(R.string.article_phases_title),
                context.getString(R.string.article_phases_summary),
                strings(context,
                        R.string.article_phases_p1,
                        R.string.article_phases_p2,
                        R.string.article_phases_p3),
                Arrays.asList(
                        new InfoArticle.ArticleSource("Cleveland Clinic - Follicular phase", "https://my.clevelandclinic.org/health/body/23953-follicular-phase"),
                        new InfoArticle.ArticleSource("Cleveland Clinic - Luteal phase", "https://my.clevelandclinic.org/health/body/24417-luteal-phase"),
                        new InfoArticle.ArticleSource("StatPearls - Physiology, Ovulation", "https://www.ncbi.nlm.nih.gov/books/NBK546686/")
                )
        );
    }

    private static InfoArticle buildPmsArticle(Context context) {
        return new InfoArticle(
                "symptomes_premenstruels",
                InfoArticle.CATEGORY_CYCLE,
                context.getString(R.string.article_pms_title),
                context.getString(R.string.article_pms_summary),
                strings(context,
                        R.string.article_pms_p1,
                        R.string.article_pms_p2,
                        R.string.article_pms_p3),
                Arrays.asList(
                        new InfoArticle.ArticleSource("ACOG - Premenstrual Syndrome", "https://www.acog.org/womens-health/faqs/premenstrual-syndrome"),
                        new InfoArticle.ArticleSource("NHS - PMS", "https://www.nhs.uk/conditions/pre-menstrual-syndrome/"),
                        new InfoArticle.ArticleSource("Cleveland Clinic - PMDD", "https://my.clevelandclinic.org/health/diseases/9132-premenstrual-dysphoric-disorder-pmdd")
                )
        );
    }

    private static InfoArticle buildPeriodsArticle(Context context) {
        return new InfoArticle(
                "regles",
                InfoArticle.CATEGORY_CYCLE,
                context.getString(R.string.article_periods_title),
                context.getString(R.string.article_periods_summary),
                strings(context,
                        R.string.article_periods_p1,
                        R.string.article_periods_p2,
                        R.string.article_periods_p3),
                Arrays.asList(
                        new InfoArticle.ArticleSource("NHS - Heavy periods", "https://www.nhs.uk/conditions/heavy-periods/"),
                        new InfoArticle.ArticleSource("Mayo Clinic - Menstrual cramps", "https://www.mayoclinic.org/diseases-conditions/menstrual-cramps/symptoms-causes/syc-20374938"),
                        new InfoArticle.ArticleSource("Cleveland Clinic - Menorrhagia", "https://my.clevelandclinic.org/health/diseases/17734-menorrhagia")
                )
        );
    }

    private static List<String> strings(Context context, int... ids) {
        List<String> result = new ArrayList<>(ids.length);
        for (int id : ids) {
            result.add(context.getString(id));
        }
        return result;
    }
}
