package unipi.protal.smartgreecealert.settings;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import unipi.protal.smartgreecealert.R;
import unipi.protal.smartgreecealert.utils.LanguageUtils;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_settings);
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

    @Override
    protected void onResume() {
        super.onResume();
        SharedPrefsUtils.updateLanguage(this, getResources(), SharedPrefsUtils.getCurrentLanguage(this));
        setTitle(getString(R.string.language_setting));
    }
}