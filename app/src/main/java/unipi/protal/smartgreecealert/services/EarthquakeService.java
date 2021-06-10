package unipi.protal.smartgreecealert.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class EarthquakeService extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor accelerometerSensor;
    private static final String EARTHQUAKE_RECEIVER = "accelerometer_earthquake_receiver";

    public EarthquakeService() {
    }

    @Override
    public void onCreate() {
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
            if (event.values[0] + event.values[1] + event.values[2] > 10) {
                Intent intent = new Intent();
                intent.setAction(EARTHQUAKE_RECEIVER);
                sendBroadcast(intent);
            }
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
        sensorManager.registerListener(EarthquakeService.this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        return super.onStartCommand(intent, flags, startId);
    }

    /*
     * Unregister listener when service stops
     */
    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(EarthquakeService.this, accelerometerSensor);
        super.onDestroy();
    }

}
