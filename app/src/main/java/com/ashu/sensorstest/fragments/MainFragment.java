package com.ashu.sensorstest.fragments;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.ashu.sensorstest.R;
import com.ashu.sensorstest.data.Data_for_graphsDBHelper;
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

import static android.content.Context.SENSOR_SERVICE;

public class MainFragment extends Fragment {

    private Sensor sensorGyroscope;
    private Sensor sensorAccelerometer;

    private LineChart mChart_1;
    private LineChart mChart_2;
    private ProgressBar progressBar;
    private LinearLayout llChart;
    private LineData data;

    private Data_for_graphsDBHelper mDbHelper_Graphs = new Data_for_graphsDBHelper(getContext());
    private ScheduledExecutorService Schedule_Load_Data;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        this.setRetainInstance(true);

        mChart_1 = v.findViewById(R.id.LineChart_1);
        mChart_2 = v.findViewById(R.id.LineChart_2);
        progressBar = v.findViewById(R.id.progressBar);
        llChart = v.findViewById(R.id.llChart);

        //проверяем наличие датчиков
        SensorManager sensorManagerService = (SensorManager) Objects.requireNonNull(getActivity()).getSystemService(SENSOR_SERVICE);
        if (sensorManagerService != null) {
            sensorGyroscope = sensorManagerService.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorAccelerometer = sensorManagerService.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }

        //проверяем наличие/создаем локальную БД
        mDbHelper_Graphs = new Data_for_graphsDBHelper(getContext());
        mDbHelper_Graphs.createDataBase();
        mDbHelper_Graphs.openDataBase();
        mDbHelper_Graphs.close();


        //оставил метод остановки сервиса чтобы не удалять каждый раз приложение.
        /*Button stopp = v.findViewById(R.id."ButtonName");
        stopp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMyServiceRunning(MainService.class)){
                    Objects.requireNonNull(getActivity()).stopService(new Intent(getContext(), MainService.class));
                }

            }
        });*/

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        //при первом запуске прячем пустые графики и показываем прогресс бар
        if (data == null){
            progressBar.setVisibility(View.VISIBLE);
            llChart.setVisibility(View.INVISIBLE);
        }

        Schedule_Load_Data = Executors.newScheduledThreadPool(1);
        Schedule_Load_Data.scheduleAtFixedRate(new Runnable() {
            public void run(){

                LineData data1 = null;
                LineData data2 = null;
                Calendar cal = Calendar.getInstance();
                long timenow = cal.getTimeInMillis();
                String st_Interval_Step = getResources().getString(R.string.stInterval_Step);
                int Interval_Step = Integer.parseInt(st_Interval_Step);

                //если датчик есть, то делаем запрос данных в БД
                if(!(sensorGyroscope == null)){
                    Load_Data_from_DB(getResources().getString(R.string.GYROSCOPE), timenow, Interval_Step);
                    data1 = data;
                }

                if (!(sensorAccelerometer == null)){
                    Load_Data_from_DB(getResources().getString(R.string.ACCELERATION), timenow, Interval_Step);
                    data2 = data;
                }

                Drawing_Graphs(data1, data2);
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    //Рисуем графики
    private void Drawing_Graphs(final LineData data_1, final LineData data_2) {

        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //если есть датчик и данные для него, то запускаем настройку данных и отображения виджета
                if (!(sensorGyroscope == null ) && !(data_1 == null)){
                    DrawGraph1(data_1);
                }
                if(!(sensorAccelerometer == null)&& !(data_2 == null)){
                    DrawGraph2(data_2);
                }

                //когда появились данные прячем прогресс бар и показываем графики
                if(!(data_1 == null) || !(data_2 == null)){
                    progressBar.setVisibility(View.INVISIBLE);
                    llChart.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Schedule_Load_Data.shutdownNow();
    }

    //Получение данных из БД
    private void Load_Data_from_DB(String SensorType, long TimeToStart, int Interval_Step){
        ArrayList<String> Query_Result;
        List<Entry> xValue = new ArrayList<>();
        List<Entry> yValue = new ArrayList<>();
        List<Entry> zValue = new ArrayList<>();
        int counter = 0; //сетчик секунд по оси абсцисс

        /*
            Делаем запрос в базу.
            На выходе имеем массив строк.
            Из каждой строки берутся значения для X Y и Z, которые записываются в
            соответствующий массив
         */
        Query_Result = mDbHelper_Graphs.Read_DBData_for_graphs(SensorType, TimeToStart, Interval_Step);
        if (Query_Result.size() > 0 && !Query_Result.get(0).isEmpty()){
            for (int i = Query_Result.size() - 1; i >=0; i--){
                String[] separated = new String[3];
                String[] shared_request = Query_Result.get(i).split(";");
                for (int j = 0; j < 3; j++){

                    separated[j] = shared_request[j].replace(",", ".");

                }

                xValue.add(new Entry(counter, Float.parseFloat(separated[0])));
                yValue.add(new Entry(counter, Float.parseFloat(separated[1])));
                zValue.add(new Entry(counter, Float.parseFloat(separated[2])));
                counter++;
            }
            FormatDataWithTreeValue(xValue, yValue, zValue);
        }
    }

    /*
        Настраиваем линии для графиков
     */
    private void FormatDataWithTreeValue (List<Entry> x_Value, List<Entry> y_Value, List<Entry> z_Value){
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        LineDataSet dataSet = new LineDataSet(x_Value, "X"); //значения по Х
        dataSet.setColors(ChartColor.CHARTLINE_COLORS[0]); //цвет линии
        dataSet.setCircleColors(ChartColor.CHARTLINE_COLORS[0]); //цвет точек значений
        dataSets.add(dataSet); //добавляем в массив графиков

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

    //настраиваем сам элементы верхнего графика
    private void DrawGraph1(LineData data_1){
        data_1.setDrawValues(false);
        mChart_1.setDrawGridBackground(false);
        mChart_1.getDescription().setEnabled(true);
        mChart_1.getDescription().setText(getResources().getString(R.string.GYROSCOPE));
        mChart_1.setDrawBorders(true);

        mChart_1.getAxisLeft().setEnabled(true);
        mChart_1.getAxisRight().setDrawAxisLine(false);
        mChart_1.getAxisRight().setDrawGridLines(false);
        mChart_1.getXAxis().setDrawAxisLine(false);
        mChart_1.getXAxis().setDrawGridLines(false);
        mChart_1.setData(data_1);

        Legend l1 = mChart_1.getLegend();
        l1.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l1.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l1.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l1.setDrawInside(false);

        mChart_1.notifyDataSetChanged();
        mChart_1.invalidate();
    }

    //настраиваем сам элементы нижнего графика
    private void DrawGraph2(LineData data_2){
        data_2.setDrawValues(false);
        mChart_2.setDrawGridBackground(false);
        mChart_2.getDescription().setEnabled(true);
        mChart_2.getDescription().setText(getResources().getString(R.string.ACCELERATION));
        mChart_2.setDrawBorders(true);

        mChart_2.getAxisLeft().setEnabled(true);
        mChart_2.getAxisRight().setDrawAxisLine(false);
        mChart_2.getAxisRight().setDrawGridLines(false);
        mChart_2.getXAxis().setDrawAxisLine(false);
        mChart_2.getXAxis().setDrawGridLines(false);
        mChart_2.setData(data_2);

        Legend l2 = mChart_2.getLegend();
        l2.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l2.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l2.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l2.setDrawInside(false);

        mChart_2.notifyDataSetChanged();
        mChart_2.invalidate();
    }

    /*
        Оставил на случай возвращения кнопки (или чего-нибудь еще) останавливающей сервис
     */
    /*private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) Objects.requireNonNull(getActivity()).getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }*/
}
