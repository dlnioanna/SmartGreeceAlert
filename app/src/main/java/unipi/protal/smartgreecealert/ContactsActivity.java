package unipi.protal.smartgreecealert;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import unipi.protal.smartgreecealert.databinding.ActivityContactsBinding;
import unipi.protal.smartgreecealert.entities.EmergencyContact;
import unipi.protal.smartgreecealert.utils.ContactsUtils;
import unipi.protal.smartgreecealert.utils.SharedPrefsUtils;

public class ContactsActivity extends AppCompatActivity {
    private ActivityContactsBinding binding;
    private List<EmergencyContact> emergencyContactArrayList;
    private Integer emergencyContactListSize = 0;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactsBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);
        emergencyContactArrayList = ContactsUtils.getSavedContacts(this);
        try {
            emergencyContactListSize = emergencyContactArrayList.size();
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        if (emergencyContactListSize != 0) {
            populateContactCards();
        } else {
            binding.cardContactFirst.setVisibility(View.GONE);
            binding.cardContactSecond.setVisibility(View.GONE);
            binding.cardContactThird.setVisibility(View.GONE);
        }
        if (emergencyContactArrayList == null || emergencyContactArrayList.size() < 3) {
            binding.addContactsButton.setOnClickListener(v -> {
                newContactDialog();
            });
        } else {
            binding.addContactsButton.setVisibility(View.GONE);
        }
        binding.deleteContactFirst.setOnClickListener(v -> {
            deleteContactDialog(0);
        });
        binding.deleteContactSecond.setOnClickListener(v -> {
            deleteContactDialog(1);
        });
        binding.deleteContactThird.setOnClickListener(v -> {
            deleteContactDialog(2);
        });
        binding.editContactFirst.setOnClickListener(v->{
            editContactDialog(0);
        });
        binding.editContactSecond.setOnClickListener(v->{
            editContactDialog(1);
        });
        binding.editContactThird.setOnClickListener(v->{
            editContactDialog(2);
        });
    }

    private void populateContactCards() {
        if (emergencyContactListSize == 1) {
            binding.addContactsTitle.setText(getString(R.string.full_contacts_title));
            binding.cardContactFirst.setVisibility(View.VISIBLE);
            binding.contactFirstName.setText(emergencyContactArrayList.get(0).getName() + " " + emergencyContactArrayList.get(0).getLastName());
            binding.contactFirstTelephone.setText(emergencyContactArrayList.get(0).getTelephone());
            binding.cardContactSecond.setVisibility(View.GONE);
            binding.cardContactThird.setVisibility(View.GONE);
            binding.addContactsButton.setVisibility(View.VISIBLE);
            binding.addContactsTitle.setText(getString(R.string.add_contacts_title));
        }else  if (emergencyContactListSize == 2) {
            binding.cardContactFirst.setVisibility(View.VISIBLE);
            binding.contactFirstName.setText(emergencyContactArrayList.get(0).getName() + " " + emergencyContactArrayList.get(0).getLastName());
            binding.contactFirstTelephone.setText(emergencyContactArrayList.get(0).getTelephone());
            binding.cardContactSecond.setVisibility(View.VISIBLE);
            binding.contactSecondName.setText(emergencyContactArrayList.get(1).getName() + " " + emergencyContactArrayList.get(1).getLastName());
            binding.contactSecondTelephone.setText(emergencyContactArrayList.get(1).getTelephone());
            binding.cardContactThird.setVisibility(View.GONE);
            binding.addContactsButton.setVisibility(View.VISIBLE);
            binding.addContactsTitle.setText(getString(R.string.add_contacts_title));
        } else  if (emergencyContactListSize == 3) {
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
        } else{
            binding.cardContactFirst.setVisibility(View.GONE);
            binding.cardContactSecond.setVisibility(View.GONE);
            binding.cardContactThird.setVisibility(View.GONE);
            binding.addContactsButton.setVisibility(View.VISIBLE);
            binding.addContactsTitle.setText(getString(R.string.add_contacts_title));
        }
    }


    /*
    method that creates an alert dialog for user to insert a new contact
     */
    private void newContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                emergencyContactArrayList = ContactsUtils.getSavedContacts(getApplicationContext());
                emergencyContactListSize = emergencyContactArrayList.size();
                populateContactCards();
            }
        });
        TextView textView = new TextView(this);
        textView.setText(getString(R.string.add_new_contact_title));
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(20F);
        textView.setTextColor(getColor(R.color.primary));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setCustomTitle(textView);
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
    }

    /*
  method that creates an alert dialog for user to delete selected contact
   */
    private void deleteContactDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                emergencyContactArrayList = ContactsUtils.getSavedContacts(getApplicationContext());
                emergencyContactListSize = emergencyContactArrayList.size();
                populateContactCards();
            }
        });
        TextView textView = new TextView(this);
        textView.setText(getString(R.string.contact_delete));
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(20F);
        textView.setTextColor(getColor(R.color.primary));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setCustomTitle(textView);
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.delete_contact, (ViewGroup) view, false);
        final TextView nameDelete = viewInflated.findViewById(R.id.name_delete);
        final TextView lastNameDelete = viewInflated.findViewById(R.id.last_name_delete);
        final TextView telephoneDelete = viewInflated.findViewById(R.id.telephone_delete);
        nameDelete.setText(getString(R.string.contact_name)+" : "+emergencyContactArrayList.get(position).getName());
        lastNameDelete.setText(getString(R.string.contact_last_name)+" : "+emergencyContactArrayList.get(position).getLastName());
        telephoneDelete.setText(getString(R.string.contact_telephone)+" : "+emergencyContactArrayList.get(position).getTelephone());
        builder.setView(viewInflated);
        // Set up dialog buttons
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EmergencyContact emergencyContact = emergencyContactArrayList.get(position);
                emergencyContactArrayList.remove(emergencyContact);
                SharedPrefsUtils.setEmergencyContacts(getApplicationContext(), emergencyContactArrayList);
                dialog.dismiss();

            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    /*
method that creates an alert dialog for user to edit an existing contact
 */
    private void editContactDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                emergencyContactArrayList = ContactsUtils.getSavedContacts(getApplicationContext());
                emergencyContactListSize = emergencyContactArrayList.size();
                populateContactCards();
            }
        });
        TextView textView = new TextView(this);
        textView.setText(getString(R.string.contact_edit));
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(20F);
        textView.setTextColor(getColor(R.color.primary));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setCustomTitle(textView);
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.new_contact, (ViewGroup) view, false);
        final EditText nameInput = viewInflated.findViewById(R.id.name_input);
        final EditText lastNameInput = viewInflated.findViewById(R.id.last_name_input);
        final EditText telephoneInput = viewInflated.findViewById(R.id.telephone_input);
        nameInput.setText(emergencyContactArrayList.get(position).getName());
        lastNameInput.setText(emergencyContactArrayList.get(position).getLastName());
        telephoneInput.setText(emergencyContactArrayList.get(position).getTelephone());
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
                    ContactsUtils.editContact(emergencyContact,position, getApplicationContext());
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPrefsUtils.updateLanguage(this, getResources(), SharedPrefsUtils.getCurrentLanguage(this));
        setTitle(getString(R.string.add_contacts));
        binding.addContactsTitle.setText(getString(R.string.add_contacts_title));
    }
}