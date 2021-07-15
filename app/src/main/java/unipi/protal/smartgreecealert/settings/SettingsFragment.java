package unipi.protal.smartgreecealert.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

import unipi.protal.smartgreecealert.R;
import unipi.protal.smartgreecealert.utils.LanguageUtils;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

public class SettingsFragment extends PreferenceFragmentCompat {
    private CheckBoxPreference greekPreference, frenchPreference, englishPreference;
    private PreferenceCategory prefTitle;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_language);
        String localeGR = getString(R.string.locale_greek);
        String localeEN = getString(R.string.locale_english);
        String localeFR = getString(R.string.locale_french);
        Resources res = getResources();
        prefTitle = this.findPreference(getString(R.string.preferences_tittle_key));
        greekPreference = this.findPreference(getString(R.string.preferences_greek_key));
        greekPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                greekPreference.setChecked(true);
                englishPreference.setChecked(false);
                frenchPreference.setChecked(false);
                SharedPrefsUtils.updateLanguage(getContext(), res, localeGR);
                restartActivity();
                return true;
            }
        });
        englishPreference = this.findPreference(getString(R.string.preferences_english_key));
        englishPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                greekPreference.setChecked(false);
                englishPreference.setChecked(true);
                frenchPreference.setChecked(false);
                SharedPrefsUtils.updateLanguage(getContext(), res, localeEN);
                restartActivity();
                return true;
            }
        });

        frenchPreference = this.findPreference(getString(R.string.preferences_french_key));
        frenchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                greekPreference.setChecked(false);
                englishPreference.setChecked(false);
                frenchPreference.setChecked(true);
                SharedPrefsUtils.updateLanguage(getContext(), res, localeFR);
                restartActivity();
                return true;
            }
        });

    }


    private void restartActivity() {
        Intent refresh = new Intent(getActivity(), getActivity()
                .getClass());
        startActivity(refresh);
        getActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPrefsUtils.updateLanguage(getActivity(), getResources(), SharedPrefsUtils.getCurrentLanguage(getActivity()));
        greekPreference.setTitle(getString(R.string.preferences_greek));
        englishPreference.setTitle(getString(R.string.preferences_english));
        frenchPreference.setTitle(getString(R.string.preferences_french));
        prefTitle.setTitle(getString(R.string.preferences_tittle));
    }
}
