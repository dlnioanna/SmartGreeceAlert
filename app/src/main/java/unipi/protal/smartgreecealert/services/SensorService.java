package unipi.protal.smartgreecealert.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import org.w3c.dom.ls.LSOutput;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import unipi.protal.smartgreecealert.entities.MovementInstance;


public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "SensorService";
    SensorManager sensorManager;
    Sensor accelerometerSensor;
    private static final String ACCELEROMETER_RECEIVER = "accelerometer_gravity_receiver";
    FallingState state;
    long freeFallTime;
    PowerConnectionReceiver powerConnectionReceiver;
    static boolean isPowerConnected;
    // EarthQuake vars
    List<MovementInstance> dataset;
    private CountDownTimer timer;
    double sampleTime;
    long datasetDuration;


    public SensorService() {
    }

    @Override
    public void onCreate() {
        // Init FallingState
        state = FallingState.INIT_STATE;
        // Power Connection Intent
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        powerConnectionReceiver = new PowerConnectionReceiver();
        registerReceiver(powerConnectionReceiver, filter);
        // Init Sensor Manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // EarthQuake vars
        dataset = new ArrayList<>();
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

            double aX = event.values[0];
            double aY = event.values[1];
            double aZ = event.values[2];

//            if(!isPowerConnected){
//                /* Call fall detection method */
//                fallDetection(acceleration);
//            }
//            /* Call earthquake detection method */
//            else earthquakeDetect(acceleration);
            //TODO: Delete the below, comment in the above
            earthquakeDetect(new MovementInstance(aX, aY, aZ));
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
                if (Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(1500)) &&
                        acceleration >= 0.90 && acceleration <= 1.10){
                    /* Detection of motion interrupts the count */
                    long duration = Instant.now().toEpochMilli() - freeFallTime;
                    /* 1500ms since free fall detection and 2500ms standing still */
                    if (duration > 4000){
                        System.out.println("IMMOBILITY_DETECTED");
                        state = FallingState.IMMOBILITY_DETECTED;
                    }
                }
                /*if motion is detected go to Initial State */
                else if(Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(1500))){
                    System.out.println("Resetting State");
                    state = FallingState.INIT_STATE;
                }
                break;

            case IMMOBILITY_DETECTED:
                /* Trigger Countdown Alarm */
                Intent intent = new Intent();
                intent.setAction(ACCELEROMETER_RECEIVER);
                sendBroadcast(intent);
                System.out.println("Alarm Triggered!!!");
                state = FallingState.INIT_STATE;
                break;
        }
    }

    public void earthquakeDetect(MovementInstance movementInstance){
        if (movementInstance.getAccelerationVector() < 0.99
                || movementInstance.getAccelerationVector() > 1.01){
            dataset.add(movementInstance);
            Log.println(Log.DEBUG, TAG, "Movement Detection! Time: " +movementInstance.getInstanceTime());
        }
        if (!dataset.isEmpty() && Instant.now().isAfter(Instant.ofEpochMilli(dataset.get(0).getInstanceTime()).plusSeconds(10))){
            if (dataset.size() > 5){
                datasetDuration = dataset.get(dataset.size()-1).getInstanceTime() - dataset.get(0).getInstanceTime();
                if (calculateIQR() && calculateZCR()){
                    Log.println(Log.DEBUG, TAG, "Earthquake Detected!!!");
                }
            }
            dataset.clear();
            datasetDuration = 0;
        }
    }

    //Interquartile range of acceleration vector sum
    private boolean calculateIQR(){
        //Sort Sample List
        Comparator<MovementInstance> comparator = Comparator.comparing(MovementInstance::getAccelerationVector);
        dataset.sort(comparator);
        //Find median index of whole dataset
        int median_idx = getMedian(0, dataset.size());
        //Find median of the first half
        double q1 = dataset.get(getMedian(0,median_idx)).getAccelerationVector();
        //Find median of the second half
        double q3 = dataset.get(getMedian(median_idx + 1, dataset.size())).getAccelerationVector();
        //IQR
        double iqr = q3-q1;

        Log.println(Log.DEBUG, TAG, "IQR -> Dataset Size: " +dataset.size()
                +", Median: " +median_idx +", Q1: " +q1 +", Q3: " +q3 +", IQR: " +iqr);

        return iqr < 0.03;
    }

    //Find index of median of an array
    private int getMedian(int left, int right){
        int median = right - left + 1;
        median = (median + 1) / 2 - 1;
        return median + left;
    }

    //Zero Crossing Rate
    private boolean calculateZCR(){
        int numCrossingsX = 0, numCrossingsY = 0, numCrossingsZ = 0;
        for (int n = 1; n < dataset.size(); n++){
            if ((dataset.get(n).getX() * dataset.get(n-1).getX()) < 0){
                numCrossingsX++;
            }
            if ((dataset.get(n).getY() * dataset.get(n-1).getY()) < 0){
                numCrossingsY++;
            }
            if ((dataset.get(n).getZ() * dataset.get(n-1).getZ()) < 0){
                numCrossingsZ++;
            }
        }
        double duration = (double) datasetDuration/1000;
        double zcrX = (double) numCrossingsX/duration;
        double zcrY = (double) numCrossingsY/duration;
        double zcrZ = (double) numCrossingsZ/duration;

        Log.println(Log.DEBUG, TAG, "ZCR -> Duration: " +duration +", ZeroCrossingX: "
                +zcrX +", ZeroCrossingY: " +zcrY +", ZeroCrossingZ: " +zcrZ);

        return (zcrX > 0.5 || zcrY > 0.5 || zcrZ > 0.5) && (zcrX < 10 && zcrY < 10 && zcrZ < 10);
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
        unregisterReceiver(powerConnectionReceiver);
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
