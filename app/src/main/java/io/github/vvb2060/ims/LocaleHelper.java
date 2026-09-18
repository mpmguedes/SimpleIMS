package io.github.vvb2060.ims;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {
    private static final String PREF_NAME = "locale_config";
    private static final String KEY_LANGUAGE = "language";

    public static void setLocale(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        updateResources(context, languageCode);
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // 如果用户已经手动设置过语言，使用用户设置
        if (prefs.contains(KEY_LANGUAGE)) {
            return prefs.getString(KEY_LANGUAGE, "en");
        }

        // 否则根据系统语言自动判断
        String systemLang = Locale.getDefault().getLanguage();
        if (systemLang.startsWith("zh")) return "zh";   // 中文
        if (systemLang.startsWith("pt")) return "pt";   // Português
        return "en";
    }

    public static void updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    /** Supported languages, in the order the switch button cycles through them. */
    private static final String[] LANGUAGES = {"en", "pt", "zh"};

    /** The language that follows {@code current} in the cycle. */
    public static String nextLanguage(String current) {
        for (int i = 0; i < LANGUAGES.length; i++) {
            if (LANGUAGES[i].equals(current)) {
                return LANGUAGES[(i + 1) % LANGUAGES.length];
            }
        }
        return "en";
    }

    /**
     * Native name of the language the button will switch to, so the label is
     * always readable regardless of the language currently displayed.
     */
    public static String nextLanguageLabel(Context context) {
        String next = nextLanguage(getLanguage(context));
        if ("pt".equals(next)) return "Português";
        if ("zh".equals(next)) return "中文";
        return "English";
    }

    public static String toggleLanguage(Context context) {
        String newLang = nextLanguage(getLanguage(context));
        setLocale(context, newLang);
        return newLang;
    }
}
