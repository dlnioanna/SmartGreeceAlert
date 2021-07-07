package unipi.protal.smartgreecealert;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import unipi.protal.smartgreecealert.databinding.ActivityAlertBinding;
import unipi.protal.smartgreecealert.databinding.ActivityStatisticsBinding;
import unipi.protal.smartgreecealert.entities.EmergencyContact;
import unipi.protal.smartgreecealert.entities.Report;
import unipi.protal.smartgreecealert.entities.ReportType;
import unipi.protal.smartgreecealert.utils.ContactsUtils;

import static unipi.protal.smartgreecealert.AlertActivity.REPORTS;
import static unipi.protal.smartgreecealert.AlertActivity.REPORT_TYPE;

public class StatisticsActivity extends AppCompatActivity {
//    private PieChart pieChart;
    private ActivityStatisticsBinding binding;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ArrayList<Report> reportArrayList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference(REPORTS);
        Query query = databaseReference.orderByChild(REPORT_TYPE).equalTo(String.valueOf(ReportType.FALL_REPORT));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                       String userId = snapshot.getKey();
                        Log.e("user id", userId);                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        setUpPieChart();
        setUpBars();
        setUpBubbles();
    }

    private void setUpBars() {
        List<EmergencyContact> emergencyContactList = ContactsUtils.getSavedContacts(getApplicationContext());
        List<BarEntry> barEntries = new ArrayList<>();
        for (EmergencyContact e : emergencyContactList) {
            barEntries.add(new BarEntry(01f, 2f));
        }
        BarDataSet barDataSet = new BarDataSet(barEntries, "whaaat");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.9f);
        binding.barChart.setVisibility(View.VISIBLE);
        binding.barChart.animateY(2500);
        binding.barChart.setData(barData);
        binding.barChart.setFitBars(true);
         Description description = new Description();
         description.setText("description text");
         binding.barChart.setDescription(description);
        binding.barChart.invalidate();
    }

    private void setUpBubbles() {
        List<EmergencyContact> emergencyContactList = ContactsUtils.getSavedContacts(getApplicationContext());
        List<BubbleEntry> bubbleEntries = new ArrayList<>();
        for (EmergencyContact e : emergencyContactList) {
            bubbleEntries.add(new BubbleEntry(1,1,1,e));
        }
        BubbleDataSet bubbleDataSet = new BubbleDataSet(bubbleEntries, "bubbles");
        bubbleDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

        BubbleData barData = new BubbleData(bubbleDataSet);
        bubbleDataSet.setHighlightCircleWidth(0.9f);
        binding.bubbleChart.setVisibility(View.VISIBLE);
        binding.bubbleChart.setData(barData);
        binding.bubbleChart.animateXY(2000,2000);

         Description description = new Description();
         description.setText("description text");
         binding.bubbleChart.setDescription(description);
        binding.bubbleChart.invalidate();
    }

    private void setUpPieChart() {
        List<EmergencyContact> emergencyContactList = ContactsUtils.getSavedContacts(getApplicationContext());
        List<PieEntry> pieEntries = new ArrayList<>();
        for (EmergencyContact e : emergencyContactList) {
            pieEntries.add(new PieEntry(01f, e.getLastName()));
        }
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.pieChart.animateXY(5000,5000);
        PieDataSet pieDataSet = new PieDataSet(pieEntries,"pie chart entries");
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