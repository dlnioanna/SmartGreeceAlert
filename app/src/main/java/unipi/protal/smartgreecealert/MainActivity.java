package unipi.protal.smartgreecealert;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import unipi.protal.smartgreecealert.services.AccelerometerService;

public class MainActivity extends AppCompatActivity {
private Intent serviceIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(this, AccelerometerService.class);


    }

    @Override
    protected void onResume() {
        startService(serviceIntent);
        super.onResume();
    }

    @Override
    protected void onStop() {
        stopService(serviceIntent);
        super.onStop();

    }
}