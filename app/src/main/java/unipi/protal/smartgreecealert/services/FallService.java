package unipi.protal.smartgreecealert.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.time.Instant;


public class FallService extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor accelerometerSensor;
    private static final String FALL_RECEIVER = "accelerometer_gravity_receiver";
    FallingState state;
    long freeFallTime;

    public FallService() {
    }

    @Override
    public void onCreate() {
        state = FallingState.INIT_STATE;
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
            /* Call Fall Detection method */
            fallDetection(acceleration);

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     * Register listener to capture movement when service starts
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(FallService.this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return super.onStartCommand(intent, flags, startId);
    }

    /*
     * Unregister listener when service stops
     */
    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(FallService.this, accelerometerSensor);
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
