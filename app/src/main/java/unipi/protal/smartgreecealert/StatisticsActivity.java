package unipi.protal.smartgreecealert;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import unipi.protal.smartgreecealert.databinding.ActivityStatisticsBinding;
import unipi.protal.smartgreecealert.entities.Report;
import unipi.protal.smartgreecealert.entities.ReportType;

import static unipi.protal.smartgreecealert.AlertActivity.REPORTS;

public class StatisticsActivity extends AppCompatActivity {
    private ActivityStatisticsBinding binding;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference, databaseReference2;
    private ArrayList<Report> reportList = new ArrayList<>();
    private ArrayList<Report> reportFallList = new ArrayList<>();
    private ArrayList<Report> reportErathquakeList = new ArrayList<>();
    private ArrayList<Report> reportFireList = new ArrayList<>();
    private ArrayList<Report> reportFalseAlarmList = new ArrayList<>();
    private List<ArrayList<Report>> allReportLists = new ArrayList<>();
    private FirebaseUser user;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(REPORTS).child(user.getUid());
        Log.e("statistics", databaseReference.toString());
        Log.e("statistics user id ", user.getUid());
        databaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //iterate through each user, ignoring their UID
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Report report = snapshot.getValue(Report.class);
                            reportList.add(report);

                            if (report.getType().equals(ReportType.EARTHQUAKE_REPORT)) {
                                reportErathquakeList.add(report);
                            } else if (report.getType().equals(ReportType.FALL_REPORT)) {
                                reportFallList.add(report);
                            } else if (report.getType().equals(ReportType.FIRE_REPORT)) {
                                reportFireList.add(report);
                            } else if (report.getType().equals(ReportType.FALSE_ALARM)) {
                                reportFalseAlarmList.add(report);
                            }
                        }
                        allReportLists.add(reportErathquakeList);
                        allReportLists.add(reportFallList);
                        allReportLists.add(reportFalseAlarmList);
                        allReportLists.add(reportErathquakeList);
                        setUpPieChart();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                }
        );

    }


    private void setUpPieChart() {
        List<PieEntry> pieEntries = new ArrayList<>();
        for ( ArrayList<Report> report : allReportLists) {
            pieEntries.add(new PieEntry(report.size(), "stat"));
            binding.pieChart.setVisibility(View.VISIBLE);
        }
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

}