package unipi.protal.smartgreecealert.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.preference.PreferenceManager;

import unipi.protal.smartgreecealert.R;

public class SharedPrefsUtils {
public static final String LANGUAGE_KEY="locale_override";
public static final String LANGUAGE_DEFAULT="el";
    public static String getCurrentLanguage(Context ctx)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String lang = prefs.getString(LANGUAGE_KEY, LANGUAGE_DEFAULT);
        return lang;
    }


    public static void updateLanguage(Context ctx, String lang)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        Configuration cfg = new Configuration();
        ctx.getResources().updateConfiguration(cfg, null);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LANGUAGE_KEY, lang);
        editor.apply();
    }
}
