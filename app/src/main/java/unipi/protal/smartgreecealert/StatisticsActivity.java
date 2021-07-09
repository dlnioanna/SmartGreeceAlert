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
    private DatabaseReference databaseReference,databaseReference2;
    private ArrayList<Report> reportList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(REPORTS);
        Log.e("statistics 1",databaseReference.toString());
        databaseReference2 = firebaseDatabase.getReference();
        Log.e("statistics 2",databaseReference.toString());





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