package unipi.protal.smartgreecealert;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.services.FallService;
import unipi.protal.smartgreecealert.settings.SettingsActivity;

public class AlertActivity extends AppCompatActivity {
    private static final String FALL_RECEIVER = "accelerometer_gravity_receiver";
    private ActivityAlertBinding binding;
    private Intent fallServiceIntent, earthquakeServiceIntent;
    private MediaPlayer player;
    private AccelerometerReceiver accelerometerReceiver;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        user = getIntent().getParcelableExtra("user");
        binding.text.setText(user.getDisplayName());
        player = MediaPlayer.create(this, R.raw.clock_sound);
        accelerometerReceiver = new AccelerometerReceiver();
        fallServiceIntent = new Intent(this, FallService.class);
        binding.abortButton.setOnClickListener(v -> {
            stopCounDown();
            binding.text.setText("abort");
            //      startService(fallServiceIntent);
        });
    }


    /**
     * On start create service so that is active when the app is running
     */
    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FALL_RECEIVER);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(accelerometerReceiver, intentFilter);
        startService(fallServiceIntent);
        super.onStart();
    }

    /**
     * When the app exits the service must stop
     */
    @Override
    protected void onStop() {
        stopService(fallServiceIntent);
        unregisterReceiver(accelerometerReceiver);
        super.onStop();
    }


    // create menu on the top left corner to sign out
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button
        int id = item.getItemId();
        if (id == R.id.action_sign_out) {
            signOut();
        } else if (id == R.id.action_change_language) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Inner class to Receive accelerometer state changes
     */
    private class AccelerometerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(FALL_RECEIVER)) {
                stopService(fallServiceIntent);
                timer = new CountDownTimer(10000, 1000) {
                    @Override
                    public void onTick(long l) {
                        binding.text.setText(String.valueOf((int) l / 1000));
                        player.start();
                        binding.abortButton.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFinish() {
                        stopCounDown();
                        binding.text.setText("finished");
                        startService(fallServiceIntent);
                    }
                };
                timer.start();
            } else if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                binding.text.setText("charging");
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                binding.text.setText("unpluged");
            }
        }
    }

    // method to sign out using AuthUI
    public void signOut() {
        AuthUI.getInstance()
                .signOut(this).addOnCompleteListener(task -> finish());
    }

    // Called when user cancels the coundown and the message is not sent
    private void stopCounDown() {
        player.stop();
        try {
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            timer.cancel();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        binding.abortButton.setVisibility(View.GONE);
        stopService(fallServiceIntent);
    }

}