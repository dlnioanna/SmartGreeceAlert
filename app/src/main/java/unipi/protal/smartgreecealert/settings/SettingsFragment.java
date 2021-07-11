package unipi.protal.smartgreecealert.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

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
        Resources res = getResources();
        greekPreference = this.findPreference(getString(R.string.preferences_greek_key));
        greekPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                greekPreference.setChecked(true);
                englishPreference.setChecked(false);
                frenchPreference.setChecked(false);
                Log.e("before selection",SharedPrefsUtils.getCurrentLanguage(getContext()));
                SharedPrefsUtils.updateLanguage(getContext(), res, localeGR);
                Log.e("after selection",SharedPrefsUtils.getCurrentLanguage(getContext()));
                restartActivity();
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
                Log.e("before selection",SharedPrefsUtils.getCurrentLanguage(getContext()));
                SharedPrefsUtils.updateLanguage(getContext(), res, localeEN);
                Log.e("after selection",SharedPrefsUtils.getCurrentLanguage(getContext()));
                restartActivity();
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
                Log.e("before selection",SharedPrefsUtils.getCurrentLanguage(getContext()));
                SharedPrefsUtils.updateLanguage(getContext(), res, localeFR);
                Log.e("after selection",SharedPrefsUtils.getCurrentLanguage(getContext()));
                restartActivity();
                return true;
            }
        });
    }

    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    private void restartActivity() {
        Intent refresh = new Intent(getActivity(), getActivity()
                .getClass());
        startActivity(refresh);
        getActivity().finish();
    }

}
