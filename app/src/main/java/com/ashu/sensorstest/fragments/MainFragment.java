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
import com.ashu.sensorstest.data.DataForGraphsDBHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

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

    private LineChart mChart1;
    private LineChart mChart2;
    private ProgressBar progressBar;
    private LinearLayout llChart;
    private LineData data;

    private DataForGraphsDBHelper mDBHelperGraphs = new DataForGraphsDBHelper(getContext());
    private ScheduledExecutorService scheduleLoadData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        this.setRetainInstance(true);

        mChart1 = v.findViewById(R.id.lineChart1);
        mChart2 = v.findViewById(R.id.lineChart2);
        progressBar = v.findViewById(R.id.progressBar);
        llChart = v.findViewById(R.id.llChart);

        //проверяем наличие датчиков
        SensorManager sensorManagerService = (SensorManager) Objects.requireNonNull(getActivity()).getSystemService(SENSOR_SERVICE);
        if (sensorManagerService != null) {
            sensorGyroscope = sensorManagerService.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorAccelerometer = sensorManagerService.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }

        //проверяем наличие/создаем локальную БД
        mDBHelperGraphs = new DataForGraphsDBHelper(getContext());
        mDBHelperGraphs.createDataBase();
        mDBHelperGraphs.openDataBase();
        mDBHelperGraphs.close();


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

        scheduleLoadData = Executors.newScheduledThreadPool(1);
        scheduleLoadData.scheduleAtFixedRate(new Runnable() {
            public void run(){

                LineData data1 = null;
                LineData data2 = null;
                Calendar cal = Calendar.getInstance();
                long timenow = cal.getTimeInMillis();
                String stIntervalStep = getResources().getString(R.string.stIntervalStep);
                int iIntervalStep = Integer.parseInt(stIntervalStep);

                //если датчик есть, то делаем запрос данных в БД
                if(!(sensorGyroscope == null)){
                    loadDataFromDB(getResources().getString(R.string.stGyroscope), timenow, iIntervalStep);
                    data1 = data;
                }

                if (!(sensorAccelerometer == null)){
                    loadDataFromDB(getResources().getString(R.string.stAcceleration), timenow, iIntervalStep);
                    data2 = data;
                }

                drawingGraphs(data1, data2);
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    //Рисуем графики
    private void drawingGraphs(final LineData data1, final LineData data2) {

        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //если есть датчик и данные для него, то запускаем настройку данных и отображения виджета
                if (!(sensorGyroscope == null ) && !(data1 == null)){
                    drawGraph1(data1);
                }
                if(!(sensorAccelerometer == null)&& !(data2 == null)){
                    drawGraph2(data2);
                }

                //когда появились данные прячем прогресс бар и показываем графики
                if(!(data1 == null) || !(data2 == null)){
                    progressBar.setVisibility(View.INVISIBLE);
                    llChart.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        scheduleLoadData.shutdownNow();
    }

    //Получение данных из БД
    private void loadDataFromDB(String stSensorType, long lTimeToStart, int iIntervalStep){
        ArrayList<String> arQueryResult;
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
        arQueryResult = mDBHelperGraphs.readDBDataForGraphs(stSensorType, lTimeToStart, iIntervalStep);
        if (arQueryResult.size() > 0 && !arQueryResult.get(0).isEmpty()){
            for (int i = arQueryResult.size() - 1; i >=0; i--){
                String[] separated = new String[3];
                String[] sharedRequest = arQueryResult.get(i).split(";");
                for (int j = 0; j < 3; j++){

                    separated[j] = sharedRequest[j].replace(",", ".");

                }

                xValue.add(new Entry(counter, Float.parseFloat(separated[0])));
                yValue.add(new Entry(counter, Float.parseFloat(separated[1])));
                zValue.add(new Entry(counter, Float.parseFloat(separated[2])));
                counter++;
            }
            formatDataWithTreeValue(xValue, yValue, zValue);
        }
    }

    /*
        Настраиваем линии для графиков
     */
    private void formatDataWithTreeValue(List<Entry> listXValue, List<Entry> listYValue, List<Entry> listZValue){
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        LineDataSet dataSet = new LineDataSet(listXValue, "X"); //значения по Х
        dataSet.setColor(getResources().getColor(R.color.colorXLine)); //цвет линии
        dataSet.setCircleColors(getResources().getColor(R.color.colorXLine)); //цвет точек значений
        dataSets.add(dataSet); //добавляем в массив графиков

        dataSet = new LineDataSet (listYValue, "Y");
        dataSet.setColors(getResources().getColor(R.color.colorYLine));
        dataSet.setCircleColors(getResources().getColor(R.color.colorYLine));
        dataSets.add(dataSet);

        dataSet = new LineDataSet(listZValue, "Z");
        dataSet.setColors(getResources().getColor(R.color.colorZLine));
        dataSet.setCircleColors(getResources().getColor(R.color.colorZLine));
        dataSets.add(dataSet);


        data = new LineData(dataSets);
        data.setDrawValues(false);
    }

    //настраиваем сам элементы верхнего графика
    private void drawGraph1(LineData data1){
        data1.setDrawValues(false);
        mChart1.setDrawGridBackground(false);
        mChart1.getDescription().setEnabled(true);
        mChart1.getDescription().setText(getResources().getString(R.string.stGyroscope));
        mChart1.setDrawBorders(true);

        mChart1.getAxisLeft().setEnabled(true);
        mChart1.getAxisRight().setDrawAxisLine(false);
        mChart1.getAxisRight().setDrawGridLines(false);
        mChart1.getXAxis().setDrawAxisLine(false);
        mChart1.getXAxis().setDrawGridLines(false);
        mChart1.setData(data1);

        Legend l1 = mChart1.getLegend();
        l1.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l1.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l1.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l1.setDrawInside(false);

        mChart1.notifyDataSetChanged();
        mChart1.invalidate();
    }

    //настраиваем сам элементы нижнего графика
    private void drawGraph2(LineData data2){
        data2.setDrawValues(false);
        mChart2.setDrawGridBackground(false);
        mChart2.getDescription().setEnabled(true);
        mChart2.getDescription().setText(getResources().getString(R.string.stAcceleration));
        mChart2.setDrawBorders(true);

        mChart2.getAxisLeft().setEnabled(true);
        mChart2.getAxisRight().setDrawAxisLine(false);
        mChart2.getAxisRight().setDrawGridLines(false);
        mChart2.getXAxis().setDrawAxisLine(false);
        mChart2.getXAxis().setDrawGridLines(false);
        mChart2.setData(data2);

        Legend l2 = mChart2.getLegend();
        l2.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l2.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l2.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l2.setDrawInside(false);

        mChart2.notifyDataSetChanged();
        mChart2.invalidate();
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
