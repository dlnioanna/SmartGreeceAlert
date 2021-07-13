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
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import unipi.protal.smartgreecealert.R;
import unipi.protal.smartgreecealert.entities.MovementInstance;


public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "SensorService";
    private static final String EARTHQUAKE_RECEIVER = "Earthquake_receiver";
    private static final String FALL_RECEIVER = "Fall_receiver";
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private FallingState state;
    private long freeFallTime;
    private PowerConnectionReceiver powerConnectionReceiver;
    static boolean isPowerConnected;
    private IntentFilter filter;
    private long datasetDuration;
    private List<MovementInstance> eqDataset;
    private List<MovementInstance> flDataset;
    private NotificationManager notificationManager;

    public SensorService() {
    }

    @Override
    public void onCreate() {
        // Init FallingState
        state = FallingState.INIT_STATE;
        // Init Sensor Manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // EarthQuake vars
        eqDataset = new ArrayList<>();
        flDataset = new ArrayList<>();
        // USB Connection BroadcastReceiver
        powerConnectionReceiver = new PowerConnectionReceiver();
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        // Power Connection Intent - Check for USB connection on Startup.
        checkUSBConnectionOnStartUp();
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

            if(!isPowerConnected){
                // Call fall detection method - argument acceleration vector
                fallDetection(new MovementInstance(aX, aY, aZ).getAccelerationVector());
                //Alternate Method - argument MovementInstance object
//                fallDetectionV2(new MovementInstance(aX, aY, aZ));
            }
            // Call earthquake detection method - argument movement object
            else earthquakeDetect(new MovementInstance(aX, aY, aZ));
        }
    }

    /* Finite State Machine Logic - Realtime Detection */
    private void fallDetection(double acceleration){
        
        switch (state){
            case INIT_STATE:
                // Acceleration considered as free fall if it has value lower than 0.42G ~ 0.63G
                if (acceleration < 0.63){
                    freeFallTime = Instant.now().toEpochMilli();
                    Log.println(Log.DEBUG, TAG, "FREE_FALL_DETECTED: " +freeFallTime);
                    state = FallingState.FREE_FALL_DETECTION_STATE;
                }
                break;

            case FREE_FALL_DETECTION_STATE:
                // Detect ground impact: > 2.02g ~ 3.10g
                if (acceleration > 2.02){
                    long impactTime = Instant.now().toEpochMilli();
                    long duration =  impactTime - freeFallTime;
                    // Measure duration between free fall incident and impact
                    if (duration > 250 && duration < 800){
                        Log.println(Log.DEBUG, TAG, "IMPACT_DETECTED - Falling Duration: "
                                +duration +" ms");
                        state = FallingState.IMPACT_DETECTION_STATE;
                    }
                    else{
                        Log.println(Log.DEBUG, TAG, "Resetting...");
                        state = FallingState.INIT_STATE;
                    }
                }
                else if (Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(800))){
                    Log.println(Log.DEBUG, TAG, "Resetting...");
                    state = FallingState.INIT_STATE;
                }
                break;

            case IMPACT_DETECTION_STATE:
                // Detect Immobility (about 1G): If stand still for over 2.5 seconds
                if (Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(1800)) &&
                        acceleration >= 0.90 && acceleration <= 1.10){
                    // Detection of motion interrupts the count
                    long duration = Instant.now().toEpochMilli() - freeFallTime;
                    // 1800ms since free fall detection and 2200ms standing still
                    if (duration > 4000){
                        Log.println(Log.DEBUG, TAG, "IMMOBILITY_DETECTED");
                        state = FallingState.IMMOBILITY_DETECTION_STATE;
                    }
                }
                // if motion is detected go to Initial State
                else if(Instant.now().isAfter(Instant.ofEpochMilli(freeFallTime).plusMillis(1800))){
                    Log.println(Log.DEBUG, TAG, "Resetting...");
                    state = FallingState.INIT_STATE;
                }
                break;

            case IMMOBILITY_DETECTION_STATE:
                // Trigger Countdown Alarm
                Intent intent = new Intent();
                intent.setAction(FALL_RECEIVER);
                sendBroadcast(intent);
                Log.println(Log.DEBUG, TAG, "Alarm Triggered!!!");
                state = FallingState.INIT_STATE;
                break;
        }
    }

    /* Finite State Machine Logic - Time Window Analyzing Detection */
    public void fallDetectionV2(MovementInstance movementInstance){
        double acceleration = movementInstance.getAccelerationVector();
        switch (state){
            case INIT_STATE:
                // Acceleration considered as free fall if it has value lower than 0.42G ~ 0.63G
                if (acceleration < 0.63){
                    state = FallingState.FREE_FALL_DETECTION_STATE;
                }
                break;
            case FREE_FALL_DETECTION_STATE:
                //Loading data to Dateset List for 4 seconds
                flDataset.add(movementInstance);
                if(Instant.now().toEpochMilli() - flDataset.get(0).getInstanceTime() > 4000){
                    Log.println(Log.DEBUG, TAG, "FREE_FALL_DETECTED");
                    state = FallingState.IMPACT_DETECTION_STATE;
                }
                break;
            case IMPACT_DETECTION_STATE:
                //Max is Impact maximum G
                MovementInstance max =  flDataset.stream().max(Comparator
                        .comparing(MovementInstance::getAccelerationVector))
                        .orElseThrow(NoSuchElementException::new);
                /* Min is free Fall minimum G: filter values before the Impact (max)
                and find if there is free fall (min) prior to impact */
                MovementInstance min = flDataset.stream()
                        .filter(s -> s.getInstanceTime() < max.getInstanceTime())
                        .min(Comparator.comparing(MovementInstance::getAccelerationVector))
                        .orElseThrow(NoSuchElementException::new);
                /* Duration should be under 0.8sec and Impact > 2.02G ~ 3.1G according to statistics.
                We calculate the duration between the lowest free fall G and the highest impact G */
                long duration = max.getInstanceTime() - min.getInstanceTime();
                if (duration > 250 && duration < 800 && max.getAccelerationVector() > 2.02){
                    Log.println(Log.DEBUG, TAG, "IMPACT_DETECTED - Falling Duration: " +duration);
                    boolean isMotionless = flDataset.stream()
                            //Get values that are 1 sec after the impact (filter out any bounces)
                            .filter(s -> s.getInstanceTime() > max.getInstanceTime() + 1000)
                            //if the remaining values show motionless behaviour then true/next state
                            .noneMatch(s -> s.getAccelerationVector() < 0.90 || s.getAccelerationVector() > 1.10);
                    if (isMotionless){
                        Log.println(Log.DEBUG, TAG, "IMMOBILITY_DETECTED");
                        state = FallingState.IMMOBILITY_DETECTION_STATE;
                    }
                    else {
                        //Clear fall dataset list and reset states if motion is detected
                        flDataset.clear();
                        //Reset
                        Log.println(Log.DEBUG, TAG, "Resetting...");
                        state = FallingState.INIT_STATE;
                    }
                }
                else {
                    //Clear fall dataset list and reset states if duration or max G is incorrect.
                    flDataset.clear();
                    //Reset
                    Log.println(Log.DEBUG, TAG, "Resetting...");
                    state = FallingState.INIT_STATE;
                }
                break;
            case IMMOBILITY_DETECTION_STATE:
                // Trigger Countdown Alarm
                Intent intent = new Intent();
                intent.setAction(FALL_RECEIVER);
                sendBroadcast(intent);
                Log.println(Log.DEBUG, TAG, "Alarm Triggered!!!");
                state = FallingState.INIT_STATE;
                break;
        }
    }

    public void earthquakeDetect(MovementInstance movementInstance){
        if (movementInstance.getAccelerationVector() < 0.985
                || movementInstance.getAccelerationVector() > 1.015){
            eqDataset.add(movementInstance);
            Log.println(Log.DEBUG, TAG, "Movement Detection! Time: " +movementInstance.getAccelerationVector());
        }
        //if time window is 5 seconds
        if (!eqDataset.isEmpty() && Instant.now().isAfter(Instant.ofEpochMilli(eqDataset.get(0).getInstanceTime()).plusSeconds(5))){
            if (eqDataset.size() > 5){
                datasetDuration = eqDataset.get(eqDataset.size()-1).getInstanceTime() - eqDataset.get(0).getInstanceTime();
                //Both IQR and ZCR must return True to trigger the earthquake report
                if (calculateIQR() && calculateZCR()){
                    Log.println(Log.DEBUG, TAG, "Earthquake Detected!!!");
                    Intent intent = new Intent();
                    intent.setAction(EARTHQUAKE_RECEIVER);
                    sendBroadcast(intent);
                }
            }
            eqDataset.clear();
            datasetDuration = 0;
        }
    }

    //Interquartile range of acceleration vector sum
    private boolean calculateIQR(){
        //Sort Sample List
        Comparator<MovementInstance> comparator = Comparator.comparing(MovementInstance::getAccelerationVector);
        eqDataset.sort(comparator);
        //Find median index of whole dataset
        int median_idx = getMedian(0, eqDataset.size());
        //Find median of the first half
        double q1 = eqDataset.get(getMedian(0,median_idx)).getAccelerationVector();
        //Find median of the second half
        double q3 = eqDataset.get(getMedian(median_idx + 1, eqDataset.size())).getAccelerationVector();
        //IQR
        double iqr = q3-q1;

        Log.println(Log.DEBUG, TAG, "IQR -> Dataset Size: " + eqDataset.size()
                +", Median: " +median_idx +", Q1: " +q1 +", Q3: " +q3 +", IQR: " +iqr);
        //IQR seems to detect consistent and acceptable for earthquake signals in range (0.0025 - 0.01)
        return iqr> 0.0025 && iqr < 0.01;
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
        for (int n = 1; n < eqDataset.size(); n++){
            if ((eqDataset.get(n).getX() * eqDataset.get(n-1).getX()) < 0){
                numCrossingsX++;
            }
            if ((eqDataset.get(n).getY() * eqDataset.get(n-1).getY()) < 0){
                numCrossingsY++;
            }
            if ((eqDataset.get(n).getZ() * eqDataset.get(n-1).getZ()) < 0){
                numCrossingsZ++;
            }
        }
        double duration = (double) datasetDuration/1000;
        double zcrX = (double) numCrossingsX/duration;
        double zcrY = (double) numCrossingsY/duration;
        double zcrZ = (double) numCrossingsZ/duration;

        Log.println(Log.DEBUG, TAG, "ZCR -> Duration: " +duration +", ZeroCrossingX: "
                +zcrX +", ZeroCrossingY: " +zcrY +", ZeroCrossingZ: " +zcrZ);
        //Hz of zero crossing (earthquakes are about 0.5Hz - 10Hz)
        return (zcrX > 0.5 || zcrY > 0.5 || zcrZ > 0.5) && (zcrX < 10 && zcrY < 10 && zcrZ < 10);
    }

    // Power Connection Intent - Check for USB connection on Startup.
    private void checkUSBConnectionOnStartUp(){
        //Notification and icon
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "765");
        Intent chargingStatus = registerReceiver(powerConnectionReceiver, filter);
        int plugged = chargingStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPowerConnected = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        if(SensorService.isPowerConnected){
            builder.setContentTitle("Earthquake Detection Enabled")
                    .setSmallIcon(R.drawable.ic_earthquake)
                    .setAutoCancel(true);
        }
        else{
            builder.setContentTitle("Fall Detection Enabled")
                    .setSmallIcon(R.drawable.ic_falling_man)
                    .setAutoCancel(true);
        }
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
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
        notificationManager.cancel(1);
        super.onDestroy();
    }
}

// Fall Detection Enumerator
enum FallingState {
    INIT_STATE,
    FREE_FALL_DETECTION_STATE,
    IMPACT_DETECTION_STATE,
    IMMOBILITY_DETECTION_STATE
}

class PowerConnectionReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)){
            SensorService.isPowerConnected = true;
            buildNotification(context,"765", "myChannel", "Earthquake Detection Enabled", null);
        }
        else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){
            SensorService.isPowerConnected = false;
            buildNotification(context,"765", "myChannel", "Fall Detection Enabled", null);
        }
    }

    public void buildNotification(Context context, String channelId, String channelName, String title, String message){
        NotificationChannel channel = new NotificationChannel(channelId, channelName,
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);

        if(SensorService.isPowerConnected){
            builder.setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_earthquake)
                    .setAutoCancel(true);
        }
        else{
            builder.setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_falling_man)
                    .setAutoCancel(true);
        }
        manager.notify(1, builder.build());
    }
}
