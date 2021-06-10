package unipi.protal.smartgreecealert.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import unipi.protal.smartgreecealert.R;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener{
    private Preference greekPreference, frenchPreference, englishPreference;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_language);
        greekPreference = this.findPreference(getString(R.string.preferences_greek_key));
        greekPreference.setOnPreferenceClickListener(this);
        englishPreference = this.findPreference(getString(R.string.preferences_english_key));
        englishPreference.setOnPreferenceClickListener(this);
        frenchPreference = this.findPreference(getString(R.string.preferences_french_key));
        frenchPreference.setOnPreferenceClickListener(this);
    }


//    @Override
//    public boolean onPreferenceChange(Preference preference, Object newValue) {
//        if(preference.getKey()==getString(R.string.preferences_greek_key)){
//            Toast.makeText(getContext(), "change language", Toast.LENGTH_SHORT).show();
//        }
//        return false;
//    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey()==getString(R.string.preferences_greek_key)){

            if(!preference.isEnabled()){
                Toast.makeText(getContext(), "greek", Toast.LENGTH_SHORT).show();
                greekPreference.setEnabled(true);
                englishPreference.setEnabled(false);
                frenchPreference.setEnabled(false);
            }
        }else if(preference.getKey()==getString(R.string.preferences_english_key)){
//            Toast.makeText(getContext(), "english", Toast.LENGTH_SHORT).show();
            if(!englishPreference.isEnabled()){
                greekPreference.setEnabled(false);
                englishPreference.setEnabled(true);
                frenchPreference.setEnabled(false);
            }
        }else if(preference.getKey()==getString(R.string.preferences_french_key)){
//            Toast.makeText(getContext(), "french", Toast.LENGTH_SHORT).show();
            if(!frenchPreference.isEnabled()){
                greekPreference.setEnabled(false);
                englishPreference.setEnabled(false);
                frenchPreference.setEnabled(true);
            }
        }
        return false;
    }
}
