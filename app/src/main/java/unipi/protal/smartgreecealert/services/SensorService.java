package unipi.protal.smartgreecealert.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.time.Instant;

import unipi.protal.smartgreecealert.R;


public class SensorService extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor accelerometerSensor;
    private static final String FALL_RECEIVER = "accelerometer_gravity_receiver";
    FallingState state;
    long freeFallTime;
    static boolean isPowerConnected;
    PowerConnectionReceiver powerConnectionReceiver;

    public SensorService() {
    }

    @Override
    public void onCreate() {
        //Init FallingState
        state = FallingState.INIT_STATE;
        //Power Connection Intent
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        powerConnectionReceiver = new PowerConnectionReceiver();
        registerReceiver(powerConnectionReceiver, filter);
        //Init Sensor Manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Capture movement and based on acceleration send broadcast to trigger message on ui
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /* Axis divided by Earth's Standard Gravity on surface 9.80665 m/s^2 */
            double gX = event.values[0] / 9.80665;
            double gY = event.values[1] / 9.80665;
            double gZ = event.values[2] / 9.80665;

            /* Vector length calculation */
            double acceleration = Math.sqrt(Math.pow(gX, 2) + Math.pow(gY, 2) + Math.pow(gZ, 2));

            if(!isPowerConnected){
                /* Call fall detection method */
                fallDetection(acceleration);
            }
            /* Call earthquake detection method */
            else earthquakeDetect(acceleration);
        }
    }

    /* Finite State Machine Logic */
    private void fallDetection(double acceleration){

        switch (state){
            case INIT_STATE:
                /* Acceleration considered as free fall if it has value between 0.42G and 0.63G */
                if (acceleration > 0.42 && acceleration < 0.63){
                    freeFallTime = Instant.now().toEpochMilli();
                    state = FallingState.FREE_FALL_DETECTED;
                    System.out.println("FREE_FALL_DETECTED " +freeFallTime);
                }

            case FREE_FALL_DETECTED:
                /* Detect ground impact: > 2.02g to 3.10g */
                if (acceleration > 2.02){
                    long impactTime = Instant.now().toEpochMilli();
                    long duration =  impactTime - freeFallTime;
                    /* Measure duration between free fall incident and impact */
                    if (duration > 400 && duration < 800){
                        System.out.println("IMPACT_DETECTED - Falling Duration: " +duration);
                        state = FallingState.IMPACT_DETECTED;
                    }
                    else state = FallingState.INIT_STATE;
                }
                else state = FallingState.INIT_STATE;
                break;

            case IMPACT_DETECTED:
                /* Detect Immobility (about 1G): If stand still for over 2.5 seconds*/
                if (Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(1000)) &&
                        acceleration >= 0.90 && acceleration <= 1.10 ){
                    /* Detection of motion interrupts the count */
                    long duration = Instant.now().toEpochMilli() - freeFallTime;
                    /* 1000ms since free fall detection and 2500ms standing still */
                    if (duration > 3500){
                        System.out.println("IMMOBILITY_DETECTED");
                        state = FallingState.IMMOBILITY_DETECTED;
                    }
                }
                /*if motion is detected go to Initial State */
                else if(Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(1000))){
                    System.out.println("Resetting State");
                    state = FallingState.INIT_STATE;
                }
                break;

            case IMMOBILITY_DETECTED:
                /* Trigger Countdown Alarm */
                Intent intent = new Intent();
                intent.setAction(FALL_RECEIVER);
                sendBroadcast(intent);
                System.out.println("Alarm Triggered!!!");
                state = FallingState.INIT_STATE;
                break;
        }
    }

    public void earthquakeDetect(double acceleration){
        System.out.println("EarthQuake Detection!");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     * Register listener to capture movement when service starts
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(SensorService.this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return super.onStartCommand(intent, flags, startId);
    }

    /*
     * Unregister listener when service stops
     */
    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(SensorService.this, accelerometerSensor);
        unregisterReceiver(powerConnectionReceiver); //TODO: Is it the correct?
        super.onDestroy();
    }
}

// Fall Detection Enumerator
enum FallingState {
    INIT_STATE,
    FREE_FALL_DETECTED,
    IMPACT_DETECTED,
    IMMOBILITY_DETECTED
}

class PowerConnectionReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)){
//            buildNotification(context,"765", "myChannel", "Power", "Charging!");
            SensorService.isPowerConnected = true;
        }
        else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){
            SensorService.isPowerConnected = false;
        }
    }

//    void buildNotification(Context context, String channelId, String channelName, String title, String message){
//        NotificationChannel channel = new NotificationChannel(channelId, channelName,
//                NotificationManager.IMPORTANCE_DEFAULT);
//        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        manager.createNotificationChannel(channel);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
//        builder.setContentTitle(title)
//                .setContentText(message)
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setAutoCancel(true);
//
//        manager.notify(1, builder.build());
//    }
}
