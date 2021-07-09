package unipi.protal.smartgreecealert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import unipi.protal.smartgreecealert.databinding.ActivityStatisticsBinding;
import unipi.protal.smartgreecealert.entities.EmergencyContact;
import unipi.protal.smartgreecealert.entities.Report;
import unipi.protal.smartgreecealert.utils.ContactsUtils;

import static unipi.protal.smartgreecealert.AlertActivity.REPORTS;

public class StatisticsActivity extends AppCompatActivity {
    private ActivityStatisticsBinding binding;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FirebaseFirestore firestore;
    private ArrayList<Report> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseDatabase = FirebaseDatabase.getInstance();
//        databaseReference = firebaseDatabase.getReference(REPORTS);
        firestore = FirebaseFirestore.getInstance();
        CollectionReference collectionReference = firestore.collection(REPORTS);
        Log.e("Statistics activity id",collectionReference.getId());
        Log.e("Statistics activity path",collectionReference.getPath());
        Query query = collectionReference.whereEqualTo("type", "FIRE_REPORT");
        Log.e("Statistics activity query 1",query.toString());

        DocumentReference docRef = collectionReference.document();
        Log.e("Statistics activity docRef id",docRef.getId());
        Log.e("Statistics activity docRef path",docRef.getPath());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("Statistics activity doc exists", "DocumentSnapshot data: " + document.getData());
                        Toast.makeText(getApplicationContext(),"eeeeeeeeee",Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("Statistics activity doc does not exist", "No such document");
                        Toast.makeText(getApplicationContext(),"aaaaaaaaaaa",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("Statistics activity error doc", "get failed with ", task.getException());
                    Toast.makeText(getApplicationContext(),"yyyyyyyyyy",Toast.LENGTH_SHORT).show();
                }
            }
        });
//               docRef .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Log.e("Statistics activity query", document.getId() + " => " + document.getData());
//                            }
//                        }else {
//                            Log.d("Statistics activity error query", "Error getting documents: ", task.getException());
//                        }
//                    }
//                });
        setUpPieChart();
    }


    private void setUpPieChart() {
        List<EmergencyContact> emergencyContactList = ContactsUtils.getSavedContacts(getApplicationContext());
        List<PieEntry> pieEntries = new ArrayList<>();
        for (EmergencyContact e : emergencyContactList) {
            pieEntries.add(new PieEntry(01f, e.getLastName()));
        }
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.pieChart.animateXY(2500, 2500);
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "pie chart entries");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        Description description = new Description();
        description.setText("description text");
        description.setTextColor(getColor(R.color.red));
        binding.pieChart.setDescription(description);
        binding.pieChart.invalidate();
    }

    private void getReportList(String id) {
        databaseReference.child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Report report = snapshot.getValue(Report.class);
                    reportList.add(report);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting hospitals failed, show a message
                Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}