package unipi.protal.smartgreecealert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.core.utilities.Utilities;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.entities.FireReport;
import unipi.protal.smartgreecealert.services.FallService;
import unipi.protal.smartgreecealert.settings.SettingsActivity;
import unipi.protal.smartgreecealert.utils.ImageUtils;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.SMS_FINANCIAL_TRANSACTIONS;

public class AlertActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    private static final String FALL_RECEIVER = "accelerometer_gravity_receiver";
    private static final String FIRE_REPORT_INSTANCE = "fire_report_instance";
    private static final String FIRE_REPORTS = "fire_reports";
    private static final String USER_ID = "user_id";
    public static final int REQUEST_PERMITIONS = 1000;
    public static final int TAKE_PICTURE = 2000;
    private ActivityAlertBinding binding;
    private boolean mapReady = false;
    private GoogleMap mMap;
    private LocationManager manager;
    private Location currentLocation;
    private LatLng position;
    private Intent fallServiceIntent, earthquakeServiceIntent;
    private MediaPlayer player;
    private AccelerometerReceiver accelerometerReceiver;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private CountDownTimer timer;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        user = getIntent().getParcelableExtra("user");
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    user = FirebaseAuth.getInstance().getCurrentUser();
                } else {
                    // User is signed out
                    user = null;
                }
            }
        };
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child(FIRE_REPORTS);
        binding.text.setText(user.getDisplayName());
        player = MediaPlayer.create(this, R.raw.clock_sound);
        accelerometerReceiver = new AccelerometerReceiver();
        fallServiceIntent = new Intent(this, FallService.class);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FALL_RECEIVER);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(accelerometerReceiver, intentFilter);
        startService(fallServiceIntent);
        binding.abortButton.setOnClickListener(v -> {
            stopCountDown(); // coundown stops
            binding.text.setText("abort");
            startService(fallServiceIntent); // service is registered again
        });
        binding.fireButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, TAKE_PICTURE);
            sendTextMessage();
        });
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if ((ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(this, SEND_SMS) != PackageManager.PERMISSION_DENIED)) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, SEND_SMS}, REQUEST_PERMITIONS);
        } else {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            currentLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        startGps();

    }


    // create menu on the top right corner
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
     Every time the user selects one option from the top right menu
     an action is being trggered
     */
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
        } else if (id == R.id.action_statistics) {
            Intent statisticsIntent = new Intent(this, StatisticsActivity.class);
            startActivity(statisticsIntent);
        }

        return super.onOptionsItemSelected(item);
    }


    // if user has not granted permission for location and sms he is signed out
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMITIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    signOut();
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
            user=firebaseAuth.getCurrentUser();
            Long firetime = System.currentTimeMillis();
            Bundle extra = data.getExtras();
            Bitmap bitmap = (Bitmap) extra.get("data");
            byte[] uploadImage = ImageUtils.encodeBitmap(bitmap);
            saveFireReportPhoto(uploadImage, firetime);

        }
    }

    // stores photo on firebase storage
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
            // if the upload of the photo is succesful saveFireReport is called and the uri of the photo is passed in as a parameter
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri uri = taskSnapshot.getUploadSessionUri();
                saveFireReport(uri, firetime);
            }
        });
    }

    // stores fire instance on realtime database and informs the user with a Toas message if the upload is succesful or not
    private void saveFireReport(Uri uri, Long time) {
        DatabaseReference dbref = firebaseDatabase.getReference(FIRE_REPORTS);
        FireReport fireReport = new FireReport(currentLocation.getLatitude(), currentLocation.getLongitude(), time, uri.toString(), false);
        dbref.child(user.getUid()).child(time.toString()).setValue(fireReport)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        Toast.makeText(getApplicationContext(), getString(R.string.fire_report_result_ok), Toast.LENGTH_SHORT).show();
                        //todo den allazei xroma kai den apothikeuei to pin
                        mMap.addMarker(new MarkerOptions().position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                                .title(getString(R.string.fire_location_title)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Toast.makeText(getApplicationContext(), getString(R.string.fire_report_result_error), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    // On every location change ui is updated
    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = location;
        Log.i("CURRENT LOCATION ",currentLocation.getLatitude()+" "+currentLocation.getLongitude());
        position = new LatLng(location.getLatitude(), location.getLongitude());
        try {
            mMap.clear();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        mMap.addMarker(new MarkerOptions().position(new LatLng(position.latitude, position.longitude)));
        CameraPosition target = CameraPosition.builder().target(position).zoom(14).build();
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
        try {
            // when map loads get current location
            position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(new LatLng(position.latitude, position.longitude)));
            // camera focus on current location
            CameraPosition target = CameraPosition.builder().target(position).zoom(14).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(target));
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(target), 2000, null);
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }


    }

    /*
   Method used by to check if the gps is enabled, if access to location is permited
    */
    private void startGps() {
        // if gps is not enabled show message that asks to enable it
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDiabledDialog();
        } else {
            // if permission is not granted ask for it
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_PERMITIONS);
            } else {
                // if permission is granted go to MapsActivity
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }

    // if gps is not enabled shows dialog that informs user to enable it from phone settings
    public void showGPSDiabledDialog() {
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
                        stopCountDown();
                        binding.text.setText("finished");
                        startService(fallServiceIntent);
                    }
                };
                timer.start();
            } else if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                binding.text.setText("charging");
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                binding.text.setText("unplugged");
            }
        }
    }

    // method to sign out using AuthUI
    public void signOut() {
        AuthUI.getInstance()
                .signOut(this).addOnCompleteListener(task -> finish());
    }

    // Called when user cancels the countdown and the message is not sent
    private void stopCountDown() {
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


    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        Log.e("on pause ", user.getDisplayName());
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("on resume ", user.getDisplayName());
    }

    @Override
    protected void onDestroy() {
        stopService(fallServiceIntent);
        unregisterReceiver(accelerometerReceiver);
        Log.e("onDestroy ", user.getDisplayName());
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.e("onStop ", user.getDisplayName());
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.e("onStart", user.getDisplayName());
        startService(fallServiceIntent);
        super.onStart();
    }

    // save data to prevent losing them on screen rotation when app is running but not shown
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable("f_user", user);
        outState.putParcelable("current_location", currentLocation);
        Log.e("on onSaveInstanceState ", user.getDisplayName());
        super.onSaveInstanceState(outState);
    }

    // restore data that have been saved on onSaveInstanceState
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        user = savedInstanceState.getParcelable("f_user");
        currentLocation = savedInstanceState.getParcelable("current_location");
        Log.e("on onRestoreInstanceState ", user.getDisplayName());
    }

    private void sendTextMessage() {
        String phoneNumber = "6932474176";
//        String phoneNumber = "6947679760";
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.fire_sms_message_0))
//                .append(currentLocation.getLongitude())
                .append(1.0)
                .append(getString(R.string.fire_sms_message_1))
//                .append(currentLocation.getLatitude())
                .append(1.0)
                .append(getString(R.string.fire_sms_message_2));
        Intent fireIntent = new Intent(getApplicationContext(), AlertActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, fireIntent, 0);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, builder.toString(), pi, null);
        Toast.makeText(getApplicationContext(), "Message Sent successfully!",
                Toast.LENGTH_LONG).show();

    }

}