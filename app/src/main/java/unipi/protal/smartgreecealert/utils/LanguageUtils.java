package unipi.protal.smartgreecealert.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageUtils  {

    public static void setLocale(Activity activity, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale localeEL = new Locale("el");
        Locale.setDefault(locale);
        Configuration config =  activity.getApplicationContext().getResources().getConfiguration();
        config.setLocale(locale);
//        activity.getApplicationContext().createConfigurationContext(config);
        activity.getApplicationContext().getResources().getDisplayMetrics();
        //resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}
