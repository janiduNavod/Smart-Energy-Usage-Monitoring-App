package com.firsttry.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI Elements
    private TextView currentDate, currentTime;
    private TextView currentMonthUnits, currentMonthCost;
    private TextView lastMonthUnits, lastMonthCost;
    private TextView voltage, current, frequency;
    private LineChart lineChart;

    // Date formatters
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd, EEEE", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    // Current date info
    private Calendar calendar = Calendar.getInstance();
    private int currentYear, currentMonth, currentDay;
    private int lastMonth, lastMonthYear;

    // Chart data
    private List<String> monthLabels = new ArrayList<>();
    private List<Entry> energyEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        setupDateAndTime();
        setupChart();
        setupFirebaseListener();
    }

    private void initializeUI() {
        // Initialize UI elements
        currentDate = findViewById(R.id.currentDate);
        currentTime = findViewById(R.id.currentTime);
        currentMonthUnits = findViewById(R.id.currentMonthUnits);
        currentMonthCost = findViewById(R.id.currentMonthCost);
        lastMonthUnits = findViewById(R.id.lastMonthUnits);
        lastMonthCost = findViewById(R.id.lastMonthCost);
        voltage = findViewById(R.id.voltage);
        current = findViewById(R.id.current);
        frequency = findViewById(R.id.frequency);
        lineChart = findViewById(R.id.lineChart);
    }

    private void setupDateAndTime() {
        // Set current date and time
        currentDate.setText(dateFormat.format(calendar.getTime()));
        currentTime.setText(timeFormat.format(calendar.getTime()));

        // Get current date components
        currentYear = calendar.get(Calendar.YEAR);
        currentMonth = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
        currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Calculate last month
        Calendar lastMonthCalendar = Calendar.getInstance();
        lastMonthCalendar.add(Calendar.MONTH, -1);
        lastMonth = lastMonthCalendar.get(Calendar.MONTH) + 1;
        lastMonthYear = lastMonthCalendar.get(Calendar.YEAR);

        // Update time every minute
        updateTimeEveryMinute();
    }

    private void setupChart() {
        // Configure the LineChart
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);

        // Configure X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12);
        xAxis.setTextColor(Color.GRAY);

        // Configure Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setGridColor(Color.LTGRAY);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure legend
        lineChart.getLegend().setEnabled(true);
        lineChart.getLegend().setTextColor(Color.GRAY);

        // Set up month labels for the past 12 months
        setupMonthLabels();
    }

    private void setupMonthLabels() {
        monthLabels.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        // Set to January of current year
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.YEAR, currentYear);

        // Add all 12 months of current year
        for (int i = 0; i < 12; i++) {
            monthLabels.add(monthFormat.format(cal.getTime()));
            cal.add(Calendar.MONTH, 1);
        }

        // Set the formatter for X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
    }

    private void updateTimeEveryMinute() {
        Thread timeUpdateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Sleep for 1 minute
                    runOnUiThread(() -> {
                        calendar = Calendar.getInstance();
                        currentTime.setText(timeFormat.format(calendar.getTime()));
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timeUpdateThread.setDaemon(true);
        timeUpdateThread.start();
    }

    private void setupFirebaseListener() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference energyDataRef = database.getReference("energy_data");

        energyDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "Firebase data updated");

                // Process current month data
                processCurrentMonthData(snapshot);

                // Process last month data
                processLastMonthData(snapshot);

                // Update real-time metrics (voltage, current, frequency)
                updateRealTimeMetrics(snapshot);

                // Update 12-month trend chart
                updateTrendChart(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void updateTrendChart(DataSnapshot snapshot) {
        energyEntries.clear();

        // Process all 12 months of current year
        for (int month = 1; month <= 12; month++) {
            double monthlyEnergy = getMonthlyEnergyConsumption(snapshot, currentYear, month);
            energyEntries.add(new Entry(month - 1, (float) monthlyEnergy)); // month-1 for 0-based index

            Log.d(TAG, "Month " + month + "/" + currentYear + ": " + monthlyEnergy + " kWh");
        }

        // Update chart on UI thread
        runOnUiThread(this::displayChart);
    }

    private double getMonthlyEnergyConsumption(DataSnapshot snapshot, int year, int month) {
        DataSnapshot yearSnapshot = snapshot.child(String.valueOf(year));
        DataSnapshot monthSnapshot = yearSnapshot.child(String.valueOf(month));

        if (!monthSnapshot.exists()) {
            return 0.0;
        }

        // Find the latest day with data in the month
        String latestDay = null;
        for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) {
            if (latestDay == null || Integer.parseInt(daySnapshot.getKey()) > Integer.parseInt(latestDay)) {
                latestDay = daySnapshot.getKey();
            }
        }

        if (latestDay != null) {
            DataSnapshot latestDaySnapshot = monthSnapshot.child(latestDay);
            DataSnapshot latestRecord = getLatestRecordFromDay(latestDaySnapshot);

            if (latestRecord != null) {
                Double energy = latestRecord.child("current_month_energy").getValue(Double.class);
                return energy != null ? energy : 0.0;
            }
        }

        return 0.0;
    }

    private void displayChart() {
        if (energyEntries.isEmpty()) {
            return;
        }

        LineDataSet dataSet = new LineDataSet(energyEntries, "Energy Consumption (kWh)");

        // Customize the line appearance
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_500));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_700));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_100));
        dataSet.setFillAlpha(50);

        // Enable smooth curves
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);

        // Animate the chart
        lineChart.animateX(1000);

        // Refresh the chart
        lineChart.invalidate();
    }

    private void processCurrentMonthData(DataSnapshot snapshot) {
        double currentMonthEnergy = 0.0;
        String latestTimestamp = "";

        DataSnapshot yearSnapshot = snapshot.child(String.valueOf(currentYear));
        DataSnapshot monthSnapshot = yearSnapshot.child(String.valueOf(currentMonth));

        if (monthSnapshot.exists()) {
            // Find the latest day with data
            String latestDay = null;
            for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) {
                if (latestDay == null || Integer.parseInt(daySnapshot.getKey()) > Integer.parseInt(latestDay)) {
                    latestDay = daySnapshot.getKey();
                }
            }

            if (latestDay != null) {
                DataSnapshot latestDaySnapshot = monthSnapshot.child(latestDay);

                // Find the latest record in that day
                DataSnapshot latestRecord = getLatestRecordFromDay(latestDaySnapshot);
                if (latestRecord != null) {
                    currentMonthEnergy = latestRecord.child("current_month_energy").getValue(Double.class);
                    latestTimestamp = latestRecord.child("timestamp").getValue(String.class);

                    Log.d(TAG, "Current month energy: " + currentMonthEnergy + " kWh");
                }
            }
        }

        // Update UI
        final double finalEnergy = currentMonthEnergy;
        runOnUiThread(() -> {
            currentMonthUnits.setText(String.format("%.1f kWh", finalEnergy));
        });
    }

    private void processLastMonthData(DataSnapshot snapshot) {
        double lastMonthEnergy = 0.0;

        DataSnapshot lastYearSnapshot = snapshot.child(String.valueOf(lastMonthYear));
        DataSnapshot lastMonthSnapshot = lastYearSnapshot.child(String.valueOf(lastMonth));

        if (lastMonthSnapshot.exists()) {
            // Find the latest day with data in last month
            String latestDay = null;
            for (DataSnapshot daySnapshot : lastMonthSnapshot.getChildren()) {
                if (latestDay == null || Integer.parseInt(daySnapshot.getKey()) > Integer.parseInt(latestDay)) {
                    latestDay = daySnapshot.getKey();
                }
            }

            if (latestDay != null) {
                DataSnapshot latestDaySnapshot = lastMonthSnapshot.child(latestDay);

                // Find the latest record in that day
                DataSnapshot latestRecord = getLatestRecordFromDay(latestDaySnapshot);
                if (latestRecord != null) {
                    lastMonthEnergy = latestRecord.child("current_month_energy").getValue(Double.class);

                    Log.d(TAG, "Last month energy: " + lastMonthEnergy + " kWh");
                }
            }
        }

        // Update UI
        final double finalEnergy = lastMonthEnergy;
        runOnUiThread(() -> {
            lastMonthUnits.setText(String.format("%.1f kWh", finalEnergy));
        });
    }

    private void updateRealTimeMetrics(DataSnapshot snapshot) {
        // Get today's data for real-time metrics
        DataSnapshot yearSnapshot = snapshot.child(String.valueOf(currentYear));
        DataSnapshot monthSnapshot = yearSnapshot.child(String.valueOf(currentMonth));
        DataSnapshot daySnapshot = monthSnapshot.child(String.valueOf(currentDay));

        if (daySnapshot.exists()) {
            // Get the latest record from today
            DataSnapshot latestRecord = getLatestRecordFromDay(daySnapshot);

            if (latestRecord != null) {
                Double voltageValue = latestRecord.child("voltage").getValue(Double.class);
                Double currentValue = latestRecord.child("current").getValue(Double.class);
                Double frequencyValue = latestRecord.child("frequency").getValue(Double.class);

                Log.d(TAG, "Real-time metrics - V: " + voltageValue + "V, I: " + currentValue + "A, F: " + frequencyValue + "Hz");

                // Update UI on main thread
                runOnUiThread(() -> {
                    if (voltageValue != null) {
                        voltage.setText(String.format("%.1f V", voltageValue));
                    }
                    if (currentValue != null) {
                        current.setText(String.format("%.1f A", currentValue));
                    }
                    if (frequencyValue != null) {
                        frequency.setText(String.format("%.1f Hz", frequencyValue));
                    }
                });
            }
        }
    }

    private DataSnapshot getLatestRecordFromDay(DataSnapshot daySnapshot) {
        DataSnapshot latestRecord = null;
        String latestTimestamp = null;

        for (DataSnapshot recordSnapshot : daySnapshot.getChildren()) {
            String timestamp = recordSnapshot.child("timestamp").getValue(String.class);

            if (timestamp != null) {
                if (latestTimestamp == null || isTimestampLater(timestamp, latestTimestamp)) {
                    latestTimestamp = timestamp;
                    latestRecord = recordSnapshot;
                }
            }
        }

        return latestRecord;
    }

    private boolean isTimestampLater(String timestamp1, String timestamp2) {
        try {
            // Parse timestamps in format "2025-5-21 11:45"
            SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault());
            return format.parse(timestamp1).after(format.parse(timestamp2));
        } catch (Exception e) {
            Log.e(TAG, "Error parsing timestamps", e);
            return false;
        }
    }
}