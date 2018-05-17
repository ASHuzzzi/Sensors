package com.ashu.sensorstest.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ashu.sensorstest.R;
import com.ashu.sensorstest.data.Data_for_graphs.Data_for_graphsDBHelper;
import com.ashu.sensorstest.data.Data_from_sensors.Data_from_sensorsDBHelper;
import com.ashu.sensorstest.services.MainService;
import com.github.mikephil.charting.charts.LineChart;
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

    TextView tv;

    final String LOG_TAG = "myLogs";

    private LineChart mChart_1;

    private ArrayList<Float> x_value;
    private double[] y_value;
    private double[] z_value;
    List<Entry> xValue;
    List<Entry> yValue;
    List<Entry> zValue;
    ArrayList<ILineDataSet> dataSetsUp = new ArrayList<ILineDataSet>();
    int counter = 0;
    View v = null;

    private Data_from_sensorsDBHelper mDbHelper_Sensors = new Data_from_sensorsDBHelper(getContext());
    private Data_for_graphsDBHelper mDbHelper_Graphs = new Data_for_graphsDBHelper(getContext());

    private ScheduledExecutorService Schedule_Load_Data;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_main, container, false);


        this.setRetainInstance(true); //1

        Button stopp = v.findViewById(R.id.btnStop);

        mDbHelper_Sensors = new Data_from_sensorsDBHelper(getContext());
        mDbHelper_Sensors.createDataBase();
        mDbHelper_Sensors.openDataBase();
        mDbHelper_Sensors.close();

        mDbHelper_Graphs = new Data_for_graphsDBHelper(getContext());
        mDbHelper_Graphs.createDataBase();
        mDbHelper_Graphs.openDataBase();
        mDbHelper_Graphs.close();
        mChart_1 = v.findViewById(R.id.LineChart_1);

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
        ArrayMap<Long, String> Query_Result;
        xValue = new ArrayList<>();
        yValue = new ArrayList<>();
        zValue = new ArrayList<>();
        int Interval_Step = 300000; //в миллисекундах. За какой промежуток будем делать выборку
        int counter = 0;
        Query_Result = mDbHelper_Graphs.Read_DBData_for_graphs("ACCELERATION", timenow, Interval_Step);
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

            Log.d(LOG_TAG, "Schedule_Load_Data");
            dataSetsUp.clear();
            LineDataSet dataSet = new LineDataSet(xValue, "xxx");
            dataSet.setColors(ChartColor.CHARTLINE_COLORS[1]);
            dataSetsUp.add(dataSet);
            dataSet = new LineDataSet (yValue, "yyy");
            dataSet.setColors(ChartColor.CHARTLINE_COLORS[2]);
            dataSetsUp.add(dataSet);
            dataSet = new LineDataSet(zValue, "zzz");
            dataSet.setColors(ChartColor.CHARTLINE_COLORS[3]);
            dataSetsUp.add(dataSet);
            LineData data = new LineData(dataSetsUp);
            data.setDrawValues(false);
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
            mChart_1.setData(data);
            mChart_1.notifyDataSetChanged();
            mChart_1.invalidate();

        }


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
                ArrayMap<Long, String> Query_Result;
                xValue = new ArrayList<>();
                yValue = new ArrayList<>();
                zValue = new ArrayList<>();
                int Interval_Step = 300000; //в миллисекундах. За какой промежуток будем делать выборку
                int counter = 0;
                Query_Result = mDbHelper_Graphs.Read_DBData_for_graphs("ACCELERATION", timenow, Interval_Step);
                if (Query_Result.size() > 0 && !Query_Result.valueAt(0).isEmpty()){
                    for (int i = 0; i<Query_Result.size(); i++){
                        String[] separated = new String[3];
                        String[] shared_request = Query_Result.valueAt(i).split(";");
                        for (int j = 0; j < 3; j++){

                            separated[j] = shared_request[j].replace(",", ".");

                        }

                        long timeee = Query_Result.keyAt(i);

                        xValue.add(new Entry(counter, Float.parseFloat(separated[0])));
                        yValue.add(new Entry(counter, Float.parseFloat(separated[1])));
                        zValue.add(new Entry(counter, Float.parseFloat(separated[2])));
                        counter++;

                    }

                    Log.d(LOG_TAG, "Schedule_Load_Data");
                    dataSetsUp.clear();
                    LineDataSet dataSet = new LineDataSet(xValue, "xxx");
                    dataSet.setColors(ChartColor.CHARTLINE_COLORS[1]);
                    dataSetsUp.add(dataSet);
                    dataSet = new LineDataSet (yValue, "yyy");
                    dataSet.setColors(ChartColor.CHARTLINE_COLORS[2]);
                    dataSetsUp.add(dataSet);
                    dataSet = new LineDataSet(zValue, "zzz");
                    dataSet.setColors(ChartColor.CHARTLINE_COLORS[3]);
                    dataSetsUp.add(dataSet);
                    Drawing_Graphs();
                }

            }
        }, 1, 1, TimeUnit.SECONDS);

    }
    public void Drawing_Graphs() {

        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                LineData data = new LineData(dataSetsUp);
                data.setDrawValues(false);
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
                mChart_1.setData(data);
                mChart_1.notifyDataSetChanged();
                mChart_1.invalidate();

            }
        });
    }



    @Override
    public void onPause() {
        super.onPause();
        Schedule_Load_Data.shutdownNow();
        mDbHelper_Graphs.close();


    }

    private long TakeTimeNow(){
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
