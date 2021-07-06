package unipi.protal.smartgreecealert;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.databinding.ActivityContactsBinding;
import unipi.protal.smartgreecealert.entities.EmergencyContact;
import unipi.protal.smartgreecealert.utils.ContactsUtils;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

public class ContactsActivity extends AppCompatActivity{
    private ActivityContactsBinding binding;
    private List<EmergencyContact> emergencyContactArrayList;
    private Integer emergecyContactListSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        emergencyContactArrayList = ContactsUtils.getSavedContacts(this);
        try {
            emergecyContactListSize = emergencyContactArrayList.size();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        if (emergecyContactListSize != 0) {
            populateContactCards();
        }else{
            binding.cardContactFirst.setVisibility(View.GONE);
            binding.cardContactSecond.setVisibility(View.GONE);
            binding.cardContactThird.setVisibility(View.GONE);
        }
        if (emergencyContactArrayList == null || emergencyContactArrayList.size() < 3) {
            binding.addContactsButton.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        emergencyContactArrayList = ContactsUtils.getSavedContacts(getApplicationContext());
                        emergecyContactListSize=emergencyContactArrayList.size();
                        populateContactCards();
                    }
                });
                builder.setTitle(getString(R.string.add_new_contact_title));
                View viewInflated = LayoutInflater.from(this).inflate(R.layout.new_contact, (ViewGroup) view, false);
                final EditText nameInput = viewInflated.findViewById(R.id.name_input);
                final EditText lastNameInput = viewInflated.findViewById(R.id.last_name_input);
                final EditText telephoneInput = viewInflated.findViewById(R.id.telephone_input);
                builder.setView(viewInflated);
                // Set up dialog buttons
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInput.getText().toString().trim();
                        String lastName = lastNameInput.getText().toString().trim();
                        String telephone = telephoneInput.getText().toString().trim();
                        if (name == null || name.equals("") || lastName == null || lastName.equals("") || telephone == null || telephone.equals("")) {
                            Toast.makeText(getApplicationContext(), getString(R.string.contact_no_values_error), Toast.LENGTH_SHORT).show();
                        } else {
                            EmergencyContact emergencyContact = new EmergencyContact(name, lastName, telephone);
                            ContactsUtils.addContact(emergencyContact, getApplicationContext());
                            dialog.dismiss();
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            });
        } else {
            binding.addContactsButton.setVisibility(View.GONE);
        }
        int[] location1 = new int[2];
        binding.cardContactFirst.getLocationOnScreen(location1);
        int x1 = location1[0];
        Toast.makeText(getApplicationContext(), "right "+x1 , Toast.LENGTH_SHORT).show();
        binding.cardContactFirst.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()){
            public void onSwipeRight() {
                int[] location = new int[2];
                binding.cardContactFirst.getLocationOnScreen(location);
                int x = location[0];
                Toast.makeText(getApplicationContext(), "right "+x , Toast.LENGTH_SHORT).show();
//
//                if(x!=0){
//                    ObjectAnimator animation = ObjectAnimator.ofFloat(binding.cardContactFirst, "translationX", 0f);
//                    animation.setDuration(500);
//                    animation.start();
//                }else {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(binding.cardContactFirst, "translationX", 300f);
                    animation.setDuration(500);
                    animation.start();
//                }
            }
            public void onSwipeLeft() {
                Toast.makeText(getApplicationContext(), "left", Toast.LENGTH_SHORT).show();
                ObjectAnimator animation = ObjectAnimator.ofFloat(binding.cardContactFirst, "translationX", -300f);
                animation.setDuration(500);
                animation.start();
            }
        });
        binding.cardContactSecond.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()){
            public void onSwipeTop() {
                Toast.makeText(getApplicationContext(), "top", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeRight() {
                Toast.makeText(getApplicationContext(), "right", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeLeft() {
                Toast.makeText(getApplicationContext(), "left", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeBottom() {
                Toast.makeText(getApplicationContext(), "bottom", Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void populateContactCards() {
        if (emergecyContactListSize == 1) {
            binding.cardContactFirst.setVisibility(View.VISIBLE);
            binding.contactFirstName.setText(emergencyContactArrayList.get(0).getName() + " " + emergencyContactArrayList.get(0).getLastName());
            binding.contactFirstTelephone.setText(emergencyContactArrayList.get(0).getTelephone());
            binding.cardContactSecond.setVisibility(View.GONE);
            binding.cardContactThird.setVisibility(View.GONE);
        }
        if (emergecyContactListSize == 2) {
            binding.cardContactFirst.setVisibility(View.VISIBLE);
            binding.contactFirstName.setText(emergencyContactArrayList.get(0).getName() + " " + emergencyContactArrayList.get(0).getLastName());
            binding.contactFirstTelephone.setText(emergencyContactArrayList.get(0).getTelephone());
            binding.cardContactSecond.setVisibility(View.VISIBLE);
            binding.contactSecondName.setText(emergencyContactArrayList.get(1).getName() + " " + emergencyContactArrayList.get(1).getLastName());
            binding.contactSecondTelephone.setText(emergencyContactArrayList.get(1).getTelephone());
            binding.cardContactThird.setVisibility(View.GONE);
        }
        if (emergecyContactListSize == 3) {
            binding.cardContactFirst.setVisibility(View.VISIBLE);
            binding.contactFirstName.setText(emergencyContactArrayList.get(0).getName() + " " + emergencyContactArrayList.get(0).getLastName());
            binding.contactFirstTelephone.setText(emergencyContactArrayList.get(0).getTelephone());
            binding.cardContactSecond.setVisibility(View.VISIBLE);
            binding.contactSecondName.setText(emergencyContactArrayList.get(1).getName() + " " + emergencyContactArrayList.get(1).getLastName());
            binding.contactSecondTelephone.setText(emergencyContactArrayList.get(1).getTelephone());
            binding.cardContactThird.setVisibility(View.VISIBLE);
            binding.contactThirdName.setText(emergencyContactArrayList.get(2).getName() + " " + emergencyContactArrayList.get(2).getLastName());
            binding.contactThirdTelephone.setText(emergencyContactArrayList.get(2).getTelephone());
            binding.addContactsButton.setVisibility(View.GONE);
            binding.addContactsTitle.setText(getString(R.string.full_contacts_title));
        }
    }


}