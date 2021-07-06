package unipi.protal.smartgreecealert.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import unipi.protal.smartgreecealert.entities.EmergencyContact;

public class ContactsUtils {
    public static void addContact(EmergencyContact emergencyContact, Context ctx) {
        List<EmergencyContact> emergencyContactList;
        if(getSavedContacts(ctx)==null){
            emergencyContactList = new ArrayList<>();
        }else {
            emergencyContactList = getSavedContacts(ctx);
        }
        emergencyContactList.add(emergencyContact);
        SharedPrefsUtils.setEmergencyContacts(ctx, emergencyContactList);
    }

    public static List<EmergencyContact> getSavedContacts(Context ctx) {
        String sharedPrefContactList = SharedPrefsUtils.getEmergencyContacts(ctx);
        Gson gson = new Gson();
        Type contactsListType = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
        ArrayList<EmergencyContact> emergencyContactArrayList = gson.fromJson(sharedPrefContactList, contactsListType);
        return emergencyContactArrayList;
    }
}
