package unipi.protal.smartgreecealert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.entities.FireReport;
import unipi.protal.smartgreecealert.services.FallService;
import unipi.protal.smartgreecealert.settings.SettingsActivity;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class AlertActivity extends AppCompatActivity implements LocationListener {
    private static final String FALL_RECEIVER = "accelerometer_gravity_receiver";
    private static final String FIRE_REPORT_INSTANCE = "fire_report_instance";
    private static final String FIRE_REPORTS = "fire_reports";
    private static final String USER_ID = "user_id";
    public static final int REQUEST_LOCATION = 1000;
    public static final int TAKE_PICTURE = 2000;
    private ActivityAlertBinding binding;
    private Intent fallServiceIntent, earthquakeServiceIntent;
    private MediaPlayer player;
    private AccelerometerReceiver accelerometerReceiver;
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private CountDownTimer timer;
    private LocationManager manager;
    private LatLng position;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        user = getIntent().getParcelableExtra("user");
        firebaseDatabase = FirebaseDatabase.getInstance();
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
            stopCountDown();
            binding.text.setText("abort");
            //      startService(fallServiceIntent);
        });
        binding.fireButton.setOnClickListener(v->{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,TAKE_PICTURE);
        });
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // todo: να ζητάει άδεια για τους χαρτες μόλις ξεκινάει ή μόλις ο χρήστης πατήσει το κουμπί fire?
        startGps();
    }

    /**
     * When the app exits the service must stop
     */
    @Override
    protected void onDestroy() {
        stopService(fallServiceIntent);
        unregisterReceiver(accelerometerReceiver);
        super.onDestroy();
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
        } else if(id == R.id.action_statistics){
            Intent statisticsIntent = new Intent(this, StatisticsActivity.class);
            startActivity(statisticsIntent);
        }

        return super.onOptionsItemSelected(item);
    }



    // if user has granted permission for location go to MapsActivity
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==TAKE_PICTURE && resultCode==RESULT_OK){
            Bundle extra = data.getExtras();
            Bitmap bitmap = (Bitmap) extra.get("data");
            binding.fireImage.setImageBitmap(rotateImage(bitmap,270));
            DatabaseReference databaseReference = firebaseDatabase.getReference(FIRE_REPORTS);
            Query query = databaseReference.orderByChild(USER_ID).equalTo(user.getUid());
            query.addListenerForSingleValueEvent(new ValueEventListener(){
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Create FireReport
                    FireReport fireReport= new FireReport(1.0,1.0, System.currentTimeMillis(),encodeBitmap(bitmap),false);
                    databaseReference.child(user.getUid()).child(FIRE_REPORT_INSTANCE).setValue(fireReport);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    Bitmap rotateImage(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),matrix,true);
    }
    public String encodeBitmap(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        return imageEncoded;
    }
    @Override
    public void onLocationChanged(@NonNull Location location) {
        try{
            position = new LatLng(location.getLatitude(), location.getLongitude());
        }catch (Exception e){
            e.printStackTrace();
        }
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
                ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
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
}