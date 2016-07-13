package com.utyf.pmetro.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * Helper class to set language of activity and to keep track of language changes
 */
public class LanguageUpdater {
    private String mLanguage;

    /**
     * Sets language of context. It is intended to be called from onCreate method of activity.
     *
     * @param context  context that needs to set its language
     * @param language current language used in application
     */
    public LanguageUpdater(Context context, String language) {
        mLanguage = language;
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        Locale locale = new Locale(mLanguage);
        setLocale(config, locale);
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        resources.updateConfiguration(config, displayMetrics);
    }

    /**
     * Checks if language has been changed and update is needed to strings in activity. This
     * method is intended to be called from onResume method of activity
     *
     * @param language current language used in application
     * @return true if language differs from language that was used previous time, so it indicates
     * that activity needs to be recreated; false otherwise
     */
    public boolean isUpdateNeeded(String language) {
        if (!mLanguage.equals(language)) {
            mLanguage = language;
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static void setLocale(Configuration config, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        }
        else {
            config.locale = locale;
        }
    }
}
