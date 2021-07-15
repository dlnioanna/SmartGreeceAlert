package unipi.protal.smartgreecealert;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.os.ConfigurationCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.entities.EmergencyContact;
import unipi.protal.smartgreecealert.entities.Report;
import unipi.protal.smartgreecealert.entities.ReportType;

import unipi.protal.smartgreecealert.services.SensorService;
import unipi.protal.smartgreecealert.settings.SettingsActivity;
import unipi.protal.smartgreecealert.utils.ContactsUtils;
import unipi.protal.smartgreecealert.utils.ImageUtils;
import unipi.protal.smartgreecealert.utils.LanguageUtils;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.SEND_SMS;

public class AlertActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private static final String TAG = "AlertActivity";
    private static final String ACCELEROMETER_RECEIVER = "accelerometer_gravity_receiver";
    private static final String FALL_RECEIVER = "Fall_receiver";
    private static final String EARTHQUAKE_RECEIVER = "Earthquake_receiver";
    public static final String REPORTS = "reports";
    private static final String EARTHQUAKE_INCIDENTS = "earthquake_incidents";
    public static final int REQUEST_LOCATION = 1000;
    public static final int REQUEST_PERMISSIONS = 1100;
    public static final int TAKE_PICTURE = 2000;
    public static final int MILLIS = 1000;
    private static boolean TIMER_STARTED = false;
    public static final int FALL_COUNTDOWN = 30;
    private ActivityAlertBinding binding;
    private boolean mapReady = false;
    private GoogleMap mMap;
    private LocationManager manager;
    private Location currentLocation;
    private LatLng position;
    private Intent sensorServiceIntent;
    private MediaPlayer player;
    private AccelerometerReceiver accelerometerReceiver;
    private StateReceiver stateReceiver;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private CountDownTimer timer;
    private Report lastReport;
    private AtomicInteger earthquakeIncidents;
    private AtomicBoolean isAlertMessageSent;
    private AtomicBoolean isEarthquakeReportSent;
    private long seconds;
    private Intent lastState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        SharedPrefsUtils.updateLanguage(this, getResources(), SharedPrefsUtils.getCurrentLanguage(this));
        LanguageUtils.setLocale(this, SharedPrefsUtils.getCurrentLanguage(this));
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference(REPORTS);
//        binding.text.setText("Welcome " +user.getDisplayName());
//        binding.imageView.setImageResource(R.drawable.ic_emergency_call);
        player = MediaPlayer.create(this, R.raw.clock_sound);
        sensorServiceIntent = new Intent(this, SensorService.class);
        //Thread safe variables
        earthquakeIncidents = new AtomicInteger(0);
        isAlertMessageSent = new AtomicBoolean(false);
        isEarthquakeReportSent = new AtomicBoolean(false);
        //Alert type receiver: AccelerometerReceiver
        accelerometerReceiver = new AccelerometerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACCELEROMETER_RECEIVER);
        intentFilter.addAction(FALL_RECEIVER);
        intentFilter.addAction(EARTHQUAKE_RECEIVER);
        registerReceiver(accelerometerReceiver, intentFilter);
        //MainActivity Icon change and animation: StateReceiver
        stateReceiver = new StateReceiver();
        IntentFilter intentStateFilter = new IntentFilter();
        intentStateFilter.addAction(SensorService.STANDING_STATE);
        intentStateFilter.addAction(SensorService.FALLING_STATE);
        intentStateFilter.addAction(SensorService.LAYING_STATE);
        intentStateFilter.addAction(SensorService.EARTHQUAKE_STATE);
        registerReceiver(stateReceiver, intentStateFilter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        // startService(sensorServiceIntent);
        binding.abortButton.setOnClickListener(v -> {
            cancelAlarm(); // countdown stops
            binding.text.setText(getString(R.string.canceled));
        });
        binding.fireButton.setOnClickListener(v -> {
            if (currentLocation != null) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, TAKE_PICTURE);
            } else {
                Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_SHORT).show();
            }
        });
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if ((ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(this, SEND_SMS) != PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, SEND_SMS}, REQUEST_PERMISSIONS);
        } else {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            currentLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (SharedPrefsUtils.getEmergencyContacts(this) == null) {
                Intent contactsIntent = new Intent(this, ContactsActivity.class);
                startActivity(contactsIntent);
            }
        }
        startGps();
        initializeTimer(FALL_COUNTDOWN);
    }

    // create menu on the top right corner
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
     Every time the user selects one option from the top right menu
     an action is being triggered
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button
        int id = item.getItemId();
        if (id == R.id.action_add_contacts) {
            Intent contactsIntent = new Intent(this, ContactsActivity.class);
            startActivity(contactsIntent);
        } else if (id == R.id.action_change_language) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (id == R.id.action_statistics) {
            Intent statisticsIntent = new Intent(this, StatisticsActivity.class);
            startActivity(statisticsIntent);
        } else if (id == R.id.action_sign_out) {
            signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    // if user has not granted permission for location and sms he is signed out
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    signOut();
                } else {
                    if (SharedPrefsUtils.getEmergencyContacts(this) == null) {
                        Intent contactsIntent = new Intent(this, ContactsActivity.class);
                        startActivity(contactsIntent);
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*
        After the user has taken a photo he/she can upload it to the firebase. Upload is triggered by
        saveFireReportPhoto function which stores the photo to firebase storage and then saveFireReport
        is called to save data as FireInstance on realtime database
         */
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            user = firebaseAuth.getCurrentUser();
            Long fireTimestamp = System.currentTimeMillis();
            Bundle extra = data.getExtras();
            Bitmap bitmap = (Bitmap) extra.get("data");
            byte[] uploadImage = ImageUtils.encodeBitmap(bitmap);
            //Save Report to Firebase
            saveFireReportPhoto(uploadImage, fireTimestamp);
            //Send SMS
            sendTextMessage(ReportType.FIRE_REPORT);
        }
    }

    // Stores photo on firebase storage
    private void saveFireReportPhoto(byte[] uploadImage, Long firetime) {
        StorageReference photoRef = storageReference.child(user.getUid()).child(firetime.toString());
        UploadTask uploadTask = photoRef.putBytes(uploadImage);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            // in case the upload fails the user is informed by a Toast message
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getApplicationContext(), getString(R.string.fire_report_result_error), Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            // If the upload of the photo is successful saveFireReport is called and the uri of the photo is passed in as a parameter
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri uri = taskSnapshot.getUploadSessionUri();
                saveReport(ReportType.FIRE_REPORT, firetime, uri);
            }
        });
    }

    // Stores reports on realtime database and informs the user with a Toast message if the upload is successful or not
    private void saveReport(ReportType reportType, Long time, Uri... uri) {
        DatabaseReference dbRef = firebaseDatabase.getReference().child(REPORTS);
        Report report;
        switch (reportType) {
            case FIRE_REPORT:
                report = new Report(ReportType.FIRE_REPORT, currentLocation.getLatitude(),
                        currentLocation.getLongitude(), time, uri.toString(), false);
                break;
            case FALL_REPORT:
                report = new Report(ReportType.FALL_REPORT, currentLocation.getLatitude(),
                        currentLocation.getLongitude(), time, false);
                break;
            case EARTHQUAKE_REPORT:
                report = new Report(ReportType.EARTHQUAKE_REPORT, currentLocation.getLatitude(),
                        currentLocation.getLongitude(), time, false);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + reportType);
        }
        dbRef.child(user.getUid()).child(time.toString()).setValue(report)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        if (reportType.equals(ReportType.FIRE_REPORT)) {
                            Toast.makeText(getApplicationContext(), getString(R.string.fire_report_result_ok), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Toast.makeText(getApplicationContext(), getString(R.string.fire_report_result_error), Toast.LENGTH_SHORT).show();
                    }
                });
        // Keep last report in case of cancellation
        lastReport = report;
    }

    // Update report - flag as cancelled
    private void cancelReport() {
        firebaseDatabase.getReference(REPORTS)
                .child(user.getUid()).child(String.valueOf(lastReport.getDate()))
                .child("canceled").setValue(true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, lastReport.getDate() + "has successfully canceled");
                    }
                });
    }

    // On every location change ui is updated
    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = location;
        position = new LatLng(location.getLatitude(), location.getLongitude());
        try {
            mMap.clear();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        mMap.addMarker(new MarkerOptions().position(new LatLng(position.latitude, position.longitude)));
        CameraPosition target = CameraPosition.builder().target(position).zoom(12).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(target));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;
        position = new LatLng(37.9755024, 23.7351172);
        CameraPosition target = CameraPosition.builder().target(position).zoom(12).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(target));
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(target), 2000, null);
        try {
            // when map loads get current location
            position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(new LatLng(position.latitude, position.longitude)));
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(target));
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(target), 2000, null);
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
    }

    /* Method used by to check if the gps is enabled, if access to location is permitted */
    private void startGps() {
        // if gps is not enabled show message that asks to enable it
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledDialog();
        } else {
            // if permission is not granted ask for it
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            } else {
                // if permission is granted go to MapsActivity
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }

    // if gps is not enabled shows dialog that informs user to enable it from phone settings
    public void showGPSDisabledDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getString(R.string.gps_title));
        alertDialog.setMessage(getString(R.string.gps_message));
        alertDialog.setPositiveButton(getString(R.string.gps_yes), new DialogInterface.OnClickListener() {
            // if user agrees to enable gps go to system settings
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(onGPS);
            }
        }).setNegativeButton(getString(R.string.gps_no), new DialogInterface.OnClickListener() {
            // if user selects no dialog disappears
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed();
            }
        });
        alertDialog.create();
        alertDialog.show();
    }

    /**
     * Inner class to Receive accelerometer state changes
     */
    private class AccelerometerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(FALL_RECEIVER)) {
                timer.start();
                TIMER_STARTED = true;
            }
            if (intent.getAction().equals(EARTHQUAKE_RECEIVER)) {
                Toast.makeText(context.getApplicationContext(),
                        getString(R.string.earthquake_detection) +"...", Toast.LENGTH_SHORT).show();
                long eventTime = Instant.now().toEpochMilli();
                if (currentLocation != null) {
                    //Save potential earthquake incident to firebase
                    sendPotentialEarthquake(eventTime);
                    //Check for similar incidents in 10km radius
                    earthquakeIncidents.set(0);
                    isEarthquakeReportSent.set(false);
                    getEarthquakeValidation(eventTime);
                    /* Else if this is the very first incident,
                    retry after 5 seconds one more time to find more incidents. */
                    repeatEarthquakeValidationAsync(eventTime);
                } else {
                    Toast.makeText(context.getApplicationContext(),
                            "No GPS connection", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //Animate MainActivity Icon
    private class StateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            lastState = intent;
            switch (intent.getAction()){
                case SensorService.STANDING_STATE:
                    Log.d(TAG, SensorService.STANDING_STATE);
                    binding.text.setText(getString(R.string.fall_detection));
                    binding.imageView.setImageResource(R.drawable.img_standing_man);
                    break;
                case SensorService.FALLING_STATE:
                    Log.d(TAG, SensorService.FALLING_STATE);
                    binding.text.setText(getString(R.string.fall_detected));
                    binding.imageView.setImageResource(R.drawable.img_falling_man);
                    break;
                case SensorService.LAYING_STATE:
                    Log.d(TAG, SensorService.LAYING_STATE);
                    binding.text.setText(getString(R.string.immobility_detected));
                    binding.imageView.setImageResource(R.drawable.img_laying_man);
                    break;
                case SensorService.EARTHQUAKE_STATE:
                    Log.d(TAG, SensorService.EARTHQUAKE_STATE);
                    binding.text.setText(getString(R.string.earthquake_detection));
                    binding.imageView.setImageResource(R.drawable.img_earthquake);
                    break;
            }
        }
    }

    private void initializeTimer(int sec) {
        binding.timerProgressBar.setMax(FALL_COUNTDOWN);
        timer = new CountDownTimer(sec * MILLIS, MILLIS) {
            @Override
            public void onTick(long leftTimeInMilliseconds) {
                seconds = leftTimeInMilliseconds / MILLIS;
                player.start();
                binding.abortButton.setVisibility(View.VISIBLE);
                binding.fireButton.setVisibility(View.GONE);
                binding.timerText.setVisibility(View.VISIBLE);
                binding.timerProgressBar.setVisibility(View.VISIBLE);
                binding.timerText.setText(String.valueOf((long) seconds));
                binding.timerProgressBar.setProgress((int) seconds, true);
            }

            @Override
            public void onFinish() {
                cancelAlarm();
                binding.timerText.setVisibility(View.GONE);
                binding.timerProgressBar.setVisibility(View.GONE);
                binding.text.setText("finished");
                binding.text.setText((!lastState.getAction().equals(SensorService.EARTHQUAKE_STATE)?
                        getString(R.string.fall_detection): getString(R.string.earthquake_detection)));
                //Get epochTime of the incident
                long incidentTime = Instant.now().toEpochMilli();
                //Create a report object
                lastReport = new Report(ReportType.FALL_REPORT, incidentTime);
                //Send SMS and Save report to Firebase Async
                sendFallReportAsync(incidentTime);
                // after timer finishes gets ready to countdown next fall
                initializeTimer(FALL_COUNTDOWN);
                TIMER_STARTED = false;
            }
        };
    }

    /* Send async sms - save fall report to firebase,
    if GPS signal is present, the system sends the message and saves report to firebase,
    if GPS signal is not present, the system checks the signal status every 15 seconds */
    @WorkerThread
    private void sendFallReportAsync(long timeOfIncident) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (currentLocation != null) {
                    //Save report to Firebase
                    saveReport(ReportType.FALL_REPORT, timeOfIncident);
                    //Send SMS
                    sendTextMessage(ReportType.FALL_REPORT);
                }
                while (currentLocation == null) {
                    Log.println(Log.DEBUG, TAG, "Waiting for GPS signal...");
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (currentLocation != null) {
                        //Call sendReportMessageHandler back in UIThread
                        //Save report to Firebase
                        saveReport(ReportType.FALL_REPORT, timeOfIncident);
                        //Send SMS
                        sendTextMessage(ReportType.FALL_REPORT);
                        break;
                    }
                }
            }
        });
        thread.start();
    }

    // Save potential earthquake incidents in separate firebase node
    private void sendPotentialEarthquake(long eventTime) {
        DatabaseReference dbRef = firebaseDatabase.getReference().child(EARTHQUAKE_INCIDENTS);
        dbRef.push().setValue(new Report(ReportType.EARTHQUAKE_REPORT, currentLocation.getLatitude(),
                currentLocation.getLongitude(), eventTime, false))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                    }
                });
    }

    /* If there are more than 3 earthquake incidents in the same minute and in 10km radius, then
    save report to database and send SMS */
    public void getEarthquakeValidation(long eventTime) {
        DatabaseReference dbRef = firebaseDatabase.getReference().child(EARTHQUAKE_INCIDENTS);
        //Get earthquake records of the last minute
        Query query = dbRef.orderByChild("date").startAfter(Instant.now().minusSeconds(60).toEpochMilli());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Log.println(Log.DEBUG, TAG, dataSnapshot.toString());
                    Report report = dataSnapshot.getValue(Report.class);
                    Location location = new Location("target");
                    location.setLatitude(report.getLatitude());
                    location.setLongitude(report.getLongitude());
                    //Distance is less or equal to 10km
                    if (currentLocation != null && currentLocation.distanceTo(location) <= 10000) {
                        earthquakeIncidents.getAndIncrement(); //earthquakeIncidents += 1
                    }
                }
                Log.println(Log.DEBUG, TAG, "Incidents in 10km range: " + earthquakeIncidents);
                //If 3 or more incidents have been detected, send earthquake report and SMS
                if (earthquakeIncidents.get() > 2 && !isEarthquakeReportSent.get()) {
                    //If there is an earthquake, save report to firebase and send SMS.
                    saveReport(ReportType.EARTHQUAKE_REPORT, eventTime);
                    sendTextMessage(ReportType.EARTHQUAKE_REPORT);
                    isEarthquakeReportSent.set(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //On Cancelled
            }
        });
    }

    // Retry after 5 seconds one more time to find more incidents.
    private void repeatEarthquakeValidationAsync(long eventTime) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (earthquakeIncidents.get() == 1 && !isEarthquakeReportSent.get()) {
                    getEarthquakeValidation(eventTime);
                }
            }
        });
        thread.start();
    }

    // method to sign out using AuthUI
    public void signOut() {
        AuthUI.getInstance()
                .signOut(this).addOnCompleteListener(task -> finish());
    }

    /* Called when user cancels the countdown and the message is not sent
    or if the message is already sent, sends a cancellation message */
    private void cancelAlarm() {
        binding.timerProgressBar.setVisibility(View.GONE);
        binding.timerText.setVisibility(View.GONE);
        binding.abortButton.setVisibility(View.GONE);
        binding.fireButton.setVisibility(View.VISIBLE);
        binding.imageView.setImageResource((!lastState.getAction()
                .equals(SensorService.EARTHQUAKE_STATE))?
                R.drawable.img_standing_man:R.drawable.img_earthquake);
        if (!isAlertMessageSent.get()) {
            player.stop();
            try {
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                timer.cancel();
                initializeTimer(FALL_COUNTDOWN);
                TIMER_STARTED = false;
            } catch (NullPointerException ne) {
                ne.printStackTrace();
            }
            binding.abortButton.setVisibility(View.GONE);
            binding.fireButton.setVisibility(View.VISIBLE);
        } else {
            //Call cancel report method
            cancelReport();
            sendTextMessage(ReportType.FALSE_ALARM);
            isAlertMessageSent.set(false);
            binding.abortButton.setVisibility(View.GONE);
            binding.fireButton.setVisibility(View.VISIBLE);
            binding.text.setText(getString(R.string.canceled));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // if locale has changed restart the activity for change to show
    @Override
    protected void onResume() {
        super.onResume();
        SharedPrefsUtils.updateLanguage(this, getResources(), SharedPrefsUtils.getCurrentLanguage(this));
        setTitle(getString(R.string.title_activity));
        binding.fireButton.setText(getString(R.string.fire_button));
        binding.abortButton.setText(getString(R.string.cancellation_button));
        try {
            binding.text.setText((!lastState.getAction().equals(SensorService.EARTHQUAKE_STATE) ?
                    getString(R.string.fall_detection) : getString(R.string.earthquake_detection)));
        }catch (NullPointerException ne){
            ne.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        stopService(sensorServiceIntent);
        unregisterReceiver(accelerometerReceiver);
        unregisterReceiver(stateReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        startService(sensorServiceIntent);
        SharedPrefsUtils.updateLanguage(this, getResources(), SharedPrefsUtils.getCurrentLanguage(this));
        super.onStart();
    }

    // save data to prevent losing them on screen rotation when app is running but not shown
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("timeProgressBar_visibility", binding.timerProgressBar.getVisibility());
        outState.putInt("timerText_visibility", binding.timerText.getVisibility());
        outState.putInt("abort_button_visibility", binding.abortButton.getVisibility());
        outState.putLong("timer_seconds", seconds);
        outState.putInt("progress", binding.timerProgressBar.getProgress());
        outState.putParcelable("f_user", user);
        outState.putParcelable("current_location", currentLocation);
        outState.putBoolean("timer_state", TIMER_STARTED);
        timer.cancel();
        super.onSaveInstanceState(outState);
    }

    // restore data that have been saved on onSaveInstanceState
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        binding.timerProgressBar.setMax(FALL_COUNTDOWN);
        binding.timerProgressBar.setVisibility(savedInstanceState.getInt("timeProgressBar_visibility"));
        binding.timerText.setVisibility(savedInstanceState.getInt("timerText_visibility"));
        binding.abortButton.setVisibility(savedInstanceState.getInt("abort_button_visibility"));
        binding.timerText.setText(String.valueOf((long) savedInstanceState.getLong("timer_seconds")));
        binding.timerProgressBar.setProgress((int) savedInstanceState.getLong("timer_seconds"), true);
        user = savedInstanceState.getParcelable("f_user");
        currentLocation = savedInstanceState.getParcelable("current_location");
        if (savedInstanceState.getLong("timer_seconds") != 0) {
            initializeTimer((int) savedInstanceState.getLong("timer_seconds"));
        } else {
            initializeTimer(FALL_COUNTDOWN);
        }
        TIMER_STARTED = savedInstanceState.getBoolean("timer_state");
        if (TIMER_STARTED) {
            timer.start();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SharedPrefsUtils.updateLanguage(this, getResources(), SharedPrefsUtils.getCurrentLanguage(this));
    }

    // Sends SMS to contacts provided by the user in shared preferences
    private void sendTextMessage(ReportType reportType) {
        List<EmergencyContact> emergencyContactList = ContactsUtils.getSavedContacts(this);
        String message = "SOS";
        String toastMessage = "Message has been sent successfully";
        switch (reportType) {
            case FIRE_REPORT:
                message = String.format(getString(R.string.fire_sms_message).toString(),
                        currentLocation.getLatitude(), currentLocation.getLongitude());
                toastMessage = getString(R.string.fire_message_sent);
                break;
            case FALL_REPORT:
                message = String.format(getString(R.string.fall_message).toString(),
                        currentLocation.getLatitude(), currentLocation.getLongitude());
                toastMessage = getString(R.string.fall_message_sent);
                break;
            case EARTHQUAKE_REPORT:
                message = String.format(getString(R.string.earthquake_message),
                        currentLocation.getLatitude(), currentLocation.getLongitude());
                toastMessage = getString(R.string.earthquake_message_sent);
                break;
            case FALSE_ALARM:
                message = getString(R.string.false_alarm);
                toastMessage = getString(R.string.false_alarm_message);
                break;
        }
        //Send SMS
        SmsManager sms = SmsManager.getDefault();
        for (EmergencyContact e : emergencyContactList) {
            sms.sendTextMessage(e.getTelephone(), null, message, null, null);
        }
        final String msg = toastMessage;
        //Run Toast in UIThread when sendTextMessage is called from a worker thread.
        runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            //Enable false alarm button
            isAlertMessageSent.set(true);
            binding.abortButton.setVisibility(View.VISIBLE);
            binding.abortButton.setText(getString(R.string.cancellation_button));
            binding.fireButton.setVisibility(View.GONE);
            binding.text.setText((!lastState.getAction().equals(SensorService.EARTHQUAKE_STATE)?
                    getString(R.string.fall_detection): getString(R.string.earthquake_detection)));
        });
    }
}