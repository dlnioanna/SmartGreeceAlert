package unipi.protal.smartgreecealert.settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Locale;

import unipi.protal.smartgreecealert.R;
import unipi.protal.smartgreecealert.utils.LanguageUtils;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.language_settings, new SettingsFragment())
                .commit();
    }


    // Update setupSharedPreferences and onSharedPreferenceChanged to load the languages
    // from shared preferences.
    private void setupSharedPreferences() {
        // Get all of the values from shared preferences to set it up
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.preferences_greek_key), true)) {
            Toast.makeText(this,"ellhnika",Toast.LENGTH_SHORT).show();
        }
        // Register the listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LanguageUtils.setLocale(this, SharedPrefsUtils.getCurrentLanguage(this));
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}