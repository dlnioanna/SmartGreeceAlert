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
import java.util.List;

import unipi.protal.smartgreecealert.databinding.ActivityStatisticsBinding;
import unipi.protal.smartgreecealert.entities.Report;
import unipi.protal.smartgreecealert.entities.ReportType;

import static unipi.protal.smartgreecealert.AlertActivity.REPORTS;

public class StatisticsActivity extends AppCompatActivity {
    private ActivityStatisticsBinding binding;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ArrayList<Report> reportFallList = new ArrayList<>();
    private ArrayList<Report> reportEarthquakeList = new ArrayList<>();
    private ArrayList<Report> reportFireList = new ArrayList<>();
    private ArrayList<Report> reportFalseAlarmList = new ArrayList<>();
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
        databaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Report report = snapshot.getValue(Report.class);
                            if (report.getType().equals(ReportType.EARTHQUAKE_REPORT)) {
                                reportEarthquakeList.add(report);
                            } else if (report.getType().equals(ReportType.FALL_REPORT)) {
                                reportFallList.add(report);
                            } else if (report.getType().equals(ReportType.FIRE_REPORT)) {
                                reportFireList.add(report);
                            } else if (report.getType().equals(ReportType.FALSE_ALARM)) {
                                reportFalseAlarmList.add(report);
                            }
                        }
                        Log.e(" report list 1",reportFallList.size()+" reportFallList");
                        Log.e(" report list 1",reportFireList.size()+" reportFireList");
                        Log.e(" report list 1", reportEarthquakeList.size()+" reportErathquakeList");
                        Log.e(" report list 1",reportFalseAlarmList.size()+" reportFalseAlarmList");
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
        if(reportFallList.size()!=0){
            pieEntries.add(new PieEntry(reportFallList.size(),getString(R.string.statistics_fall)));
        }
        if(reportFireList.size()!=0){
            pieEntries.add(new PieEntry(reportFireList.size(),getString(R.string.statistics_fire)));
        }
        if(reportEarthquakeList.size()!=0){
            pieEntries.add(new PieEntry(reportEarthquakeList.size(),getString(R.string.statistics_earthquake)));
        }
        if(reportFalseAlarmList.size()!=0){
            pieEntries.add(new PieEntry(reportFalseAlarmList.size(),getString(R.string.statistics_false_alarm)));
        }
        Log.e(" report list 2",reportFallList.size()+" reportFallList");
        Log.e(" report list 2",reportFireList.size()+" reportFireList");
        Log.e(" report list 2", reportEarthquakeList.size()+" reportErathquakeList");
        Log.e(" report list 2",reportFalseAlarmList.size()+" reportFalseAlarmList");
        binding.pieChart.animateXY(2000, 2000);
        PieDataSet pieDataSet = new PieDataSet(pieEntries,null);
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieDataSet.setValueTextColor(getColor(R.color.white));
        pieDataSet.setValueTextSize(10);
        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        Description description = new Description();
        description.setText(getString(R.string.statistics_user));
        description.setTextSize(20);
        binding.pieChart.setDrawEntryLabels(true);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setDescription(description);
        binding.pieChart.setEntryLabelColor(getColor(R.color.white));
        binding.pieChart.setEntryLabelTextSize(16);
        binding.pieChart.setHoleRadius(0f);
        binding.pieChart.setTransparentCircleRadius(0);
        binding.statisticsProgressBar.setVisibility(View.GONE);
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.pieChart.invalidate();
    }

}