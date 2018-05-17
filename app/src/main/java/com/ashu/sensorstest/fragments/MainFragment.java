package com.ashu.sensorstest.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import com.ashu.sensorstest.R;
import com.ashu.sensorstest.data.Data_for_graphs.Data_for_graphsDBHelper;
import com.ashu.sensorstest.data.Data_from_sensors.Data_from_sensorsDBHelper;
import com.ashu.sensorstest.services.MainService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.ashu.sensorstest.chart.ChartColor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainFragment extends Fragment {

    private LineChart mChart_1;
    private LineChart mChart_2;


    List<Entry> xValue;
    List<Entry> yValue;
    List<Entry> zValue;
    ArrayList<ILineDataSet> dataSets;
    View v = null;
    LineData data;

    private Data_for_graphsDBHelper mDbHelper_Graphs = new Data_for_graphsDBHelper(getContext());

    private ScheduledExecutorService Schedule_Load_Data;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_main, container, false);


        this.setRetainInstance(true); //1

        Button stopp = v.findViewById(R.id.btnStop);

        Data_from_sensorsDBHelper mDbHelper_Sensors = new Data_from_sensorsDBHelper(getContext());
        mDbHelper_Sensors.createDataBase();
        mDbHelper_Sensors.openDataBase();
        mDbHelper_Sensors.close();

        mDbHelper_Graphs = new Data_for_graphsDBHelper(getContext());
        mDbHelper_Graphs.createDataBase();
        mDbHelper_Graphs.openDataBase();
        mDbHelper_Graphs.close();
        mChart_1 = v.findViewById(R.id.LineChart_1);
        mChart_2 = v.findViewById(R.id.LineChart_2);

        stopp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMyServiceRunning(MainService.class)){
                    Objects.requireNonNull(getActivity()).stopService(new Intent(getContext(), MainService.class));
                }

            }
        });

        Calendar cal = Calendar.getInstance();
        long timenow = cal.getTimeInMillis();
        int Interval_Step = 300000;

        Load_Data_from_DB("ACCELERATION", timenow, Interval_Step);
        LineData data1 = data;

        Load_Data_from_DB("GYROSCOPE", timenow, Interval_Step);
        LineData data2 = data;

        DrawGraph1(data1);
        DrawGraph2(data2);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        Schedule_Load_Data = Executors.newScheduledThreadPool(1);
        Schedule_Load_Data.scheduleAtFixedRate(new Runnable() {
            public void run(){

                Calendar cal = Calendar.getInstance();
                long timenow = cal.getTimeInMillis();
                int Interval_Step = 300000;

                Load_Data_from_DB("ACCELERATION", timenow, Interval_Step);
                LineData data1 = data;

                Load_Data_from_DB("GYROSCOPE", timenow, Interval_Step);
                LineData data2 = data;

                Drawing_Graphs(data1, data2);
            }
        }, 1, 1, TimeUnit.SECONDS);

    }
    public void Drawing_Graphs(final LineData data_1, final LineData data_2) {

        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DrawGraph1(data_1);
                DrawGraph2(data_2);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Schedule_Load_Data.shutdownNow();
        mDbHelper_Graphs.close();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) Objects.requireNonNull(getActivity()).getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void Load_Data_from_DB(String SensorType, long TimeToStart, int Interval_Step){
        ArrayMap<Long, String> Query_Result;
        xValue = new ArrayList<>();
        yValue = new ArrayList<>();
        zValue = new ArrayList<>();
        int counter = 0;
        Query_Result = mDbHelper_Graphs.Read_DBData_for_graphs(SensorType, TimeToStart, Interval_Step);
        if (Query_Result.size() > 0 && !Query_Result.valueAt(0).isEmpty()){
            for (int i = 0; i<Query_Result.size(); i++){
                String[] separated = new String[3];
                String[] shared_request = Query_Result.valueAt(i).split(";");
                for (int j = 0; j < 3; j++){

                    separated[j] = shared_request[j].replace(",", ".");

                }

                float timeee = Query_Result.keyAt(i);

                xValue.add(new Entry(counter, Float.parseFloat(separated[0])));
                yValue.add(new Entry(counter, Float.parseFloat(separated[1])));
                zValue.add(new Entry(counter, Float.parseFloat(separated[2])));
                counter++;
            }
            FormatDataWithTreeValue(xValue, yValue, zValue);
        }
    }

    private void FormatDataWithTreeValue (List<Entry> x_Value, List<Entry> y_Value, List<Entry> z_Value){
        dataSets = new ArrayList<>();
        LineDataSet dataSet = new LineDataSet(x_Value, "X");
        dataSet.setColors(ChartColor.CHARTLINE_COLORS[0]);
        dataSet.setCircleColors(ChartColor.CHARTLINE_COLORS[0]);
        dataSets.add(dataSet);
        dataSet = new LineDataSet (y_Value, "Y");
        dataSet.setColors(ChartColor.CHARTLINE_COLORS[1]);
        dataSet.setCircleColors(ChartColor.CHARTLINE_COLORS[1]);
        dataSets.add(dataSet);
        dataSet = new LineDataSet(z_Value, "Z");
        dataSet.setColors(ChartColor.CHARTLINE_COLORS[2]);
        dataSet.setCircleColors(ChartColor.CHARTLINE_COLORS[2]);
        dataSets.add(dataSet);
        data = new LineData(dataSets);
        data.setDrawValues(false);
    }

    private void DrawGraph1(LineData data_1){
        data_1.setDrawValues(false);
        mChart_1.setDrawGridBackground(false);
        mChart_1.getDescription().setEnabled(false);
        mChart_1.setDrawBorders(true);

        mChart_1.getAxisLeft().setEnabled(true);
        mChart_1.getAxisRight().setDrawAxisLine(false);
        mChart_1.getAxisRight().setDrawGridLines(false);
        mChart_1.getXAxis().setDrawAxisLine(false);
        mChart_1.getXAxis().setDrawGridLines(false);


        //mChart_1.getXAxis().setValueFormatter(new HourAxisValueFormatter(0));
        //mChart_1.getXAxis().setValueFormatter(new DefaultAxisValueFormatter(0));
        mChart_1.setData(data_1);

        Legend l1 = mChart_1.getLegend();
        l1.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l1.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l1.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l1.setDrawInside(false);

        mChart_1.notifyDataSetChanged();
        mChart_1.invalidate();
    }

    private void DrawGraph2(LineData data_2){
        data_2.setDrawValues(false);
        mChart_2.setDrawGridBackground(false);
        mChart_2.getDescription().setEnabled(false);
        mChart_2.setDrawBorders(true);

        mChart_2.getAxisLeft().setEnabled(true);
        mChart_2.getAxisRight().setDrawAxisLine(false);
        mChart_2.getAxisRight().setDrawGridLines(false);
        mChart_2.getXAxis().setDrawAxisLine(false);
        mChart_2.getXAxis().setDrawGridLines(false);

        //mChart_1.getXAxis().setValueFormatter(new HourAxisValueFormatter(0));
        //mChart_1.getXAxis().setValueFormatter(new DefaultAxisValueFormatter(0));
        mChart_2.setData(data_2);

        Legend l2 = mChart_2.getLegend();
        l2.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l2.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l2.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l2.setDrawInside(false);

        mChart_2.notifyDataSetChanged();
        mChart_2.invalidate();
    }
}
