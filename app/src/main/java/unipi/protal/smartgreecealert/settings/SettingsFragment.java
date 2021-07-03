package unipi.protal.smartgreecealert.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

import unipi.protal.smartgreecealert.R;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

public class SettingsFragment extends PreferenceFragmentCompat {
    private CheckBoxPreference greekPreference, frenchPreference, englishPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_language);
        String localeGR = getString(R.string.locale_greek);
        String localeEN = getString(R.string.locale_english);
        String localeFR = getString(R.string.locale_french);
        greekPreference = this.findPreference(getString(R.string.preferences_greek_key));
        greekPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                greekPreference.setChecked(true);
                englishPreference.setChecked(false);
                frenchPreference.setChecked(false);
                SharedPrefsUtils.updateLanguage(getContext(),localeGR);
                return true;
            }
        });
        englishPreference = this.findPreference(getString(R.string.preferences_english_key));
        englishPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                greekPreference.setChecked(false);
                englishPreference.setChecked(true);
                frenchPreference.setChecked(false);
                SharedPrefsUtils.updateLanguage(getContext(),localeEN);
                return true;
            }
        });
        frenchPreference = this.findPreference(getString(R.string.preferences_french_key));
        frenchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                greekPreference.setChecked(false);
                englishPreference.setChecked(false);
                frenchPreference.setChecked(true);
                SharedPrefsUtils.updateLanguage(getContext(),localeFR);
                return true;
            }
        });
    }

    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }


    public CheckBoxPreference getEnglishPreference() {
        return englishPreference;
    }
//    public String getCurrentLanguage(Context ctx)
//    {
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
//        String lang = prefs.getString(getString(R.string.locale_key), getString(R.string.locale_greek));
//        return lang;
//    }
//
//    public void updateLanguage(Context ctx, String lang)
//    {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
//        Configuration cfg = new Configuration();
//        ctx.getResources().updateConfiguration(cfg, null);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString(getString(R.string.locale_key), lang);
//        editor.apply();
//    }

//    public static void changeLocale(Context context, String locale) {
//        Resources res = context.getResources();
//        Configuration conf = res.getConfiguration();
//        conf.locale = new Locale(locale);
//        res.updateConfiguration(conf, res.getDisplayMetrics());
//    }

}
