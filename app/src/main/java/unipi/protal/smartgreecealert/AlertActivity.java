package unipi.protal.smartgreecealert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.services.AccelerometerService;

public class AlertActivity extends AppCompatActivity {
    private static final String MY_APP_RECEIVER = "accelerometer_receiver";
    private ActivityAlertBinding binding;
    private Intent serviceIntent;
    private MediaPlayer player;
    private AccelerometerReceiver accelerometerReceiver;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private   CountDownTimer timer;

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
        serviceIntent = new Intent(this, AccelerometerService.class);
        binding.abortButton.setOnClickListener(v->{
            try {
                timer.cancel();
                player.stop();
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            binding.text.setText("abort");
            startService(serviceIntent);
            binding.abortButton.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MY_APP_RECEIVER);
        registerReceiver(accelerometerReceiver, intentFilter);
        startService(serviceIntent);
        super.onStart();
    }

    @Override
    protected void onStop() {
        stopService(serviceIntent);
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
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_sign_out) {
           signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    private class AccelerometerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            stopService(serviceIntent);
             timer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long l) {
                    binding.text.setText(String.valueOf((int) l / 1000));
                    player.start();
                    binding.abortButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFinish() {
                    player.stop();
                    try {
                        player.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    binding.text.setText("finished");
                    startService(serviceIntent);
                    binding.abortButton.setVisibility(View.GONE);
                }
            };
            timer.start();
        }
    }

    // method to sign out using AuthUI
    public void signOut() {
        AuthUI.getInstance()
                .signOut(this).addOnCompleteListener(task ->  finish());
    }

}