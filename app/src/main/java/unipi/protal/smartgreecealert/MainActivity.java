package unipi.protal.smartgreecealert;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import unipi.protal.smartgreecealert.databinding.ActivityMainBinding;
import unipi.protal.smartgreecealert.services.AccelerometerService;

public class MainActivity extends AppCompatActivity {
    private static final String MY_APP_RECEIVER = "accelerometer_receiver";
    private ActivityMainBinding binding;
    private Intent serviceIntent;
    private MediaPlayer player;
    AccelerometerReceiver accelerometerReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        player = MediaPlayer.create(this,R.raw.clock_sound);
        accelerometerReceiver = new AccelerometerReceiver();
        serviceIntent = new Intent(this, AccelerometerService.class);
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


    private class AccelerometerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            stopService(serviceIntent);
            CountDownTimer timer = new CountDownTimer(10000,1000) {
                @Override
                public void onTick(long l) {
                binding.text.setText(String.valueOf((int) l/1000));
                player.start();
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
                }
            };
            timer.start();
        }
    }
}