package unipi.protal.smartgreecealert;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

import unipi.protal.smartgreecealert.databinding.ActivityFirebaseUiBinding;

public class FirebaseUIActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    public static final int REQUEST_LOCATION = 1000;
    // Firebase instance variables
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseDatabase firebaseDatabase;
    private ActivityFirebaseUiBinding binding;
    private LocationManager manager;
    private FirebaseUser user;
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // bind view for better performance
        binding = ActivityFirebaseUiBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        // Initialize Firebase components
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        intent = new Intent(this, AlertActivity.class);
        // prevent from re-creating sign in ui when rotating screen or leaving screen
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    user = FirebaseAuth.getInstance().getCurrentUser();
                    intent.putExtra("user",user);
                   startActivity(intent);
                } else {
                    // User is signed out
                    createSignInIntent();
                }
            }
        };
        if(user==null){
            createSignInIntent();
        }

    }


    public void createSignInIntent() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        // Create and launch sign-in intent
        startActivity(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setLogo(R.drawable.ic_emergency_call)      // Set logo drawable
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)   //saves users credentials to phone device
                        .build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                user = FirebaseAuth.getInstance().getCurrentUser();
                intent.putExtra("user", user);
                startActivity(intent);

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise show
                Toast.makeText(this, response.getError().getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Restore auth through activity lifecycle
     */
    @Override
    protected void onResume() {
        super.onResume();
       firebaseAuth.addAuthStateListener(authStateListener);
    }

    /**
     * Keep auth through activity lifecycle
     */
    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

}
