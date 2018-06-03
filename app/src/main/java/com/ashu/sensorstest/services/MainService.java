package com.ashu.sensorstest.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.ashu.sensorstest.MainActivity;
import com.ashu.sensorstest.R;
import com.ashu.sensorstest.data.DataForGraphsDBHelper;
import com.ashu.sensorstest.sensors.Sensors;
import com.ashu.sensorstest.string_builders.StringBuilders;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainService extends Service {

    private SensorManager sensorManagerService;
    private Sensor sensorGyroscope;
    private Sensor sensorAccelerometer;

    private ArrayList<String> arrValuesGyroscope = new ArrayList<>();
    private ArrayList<String> arrValuesAcceleration = new ArrayList<>();

    private NotificationManager notificationManager;
    private static final int iDefaultNotificationId = 101;
    private PowerManager.WakeLock mWakeLock;

    private ScheduledExecutorService scheduleDataProcessing;
    private ScheduledExecutorService scheduleCleaningOfDatabases;

    private DataForGraphsDBHelper mDbHelperGraphs = new DataForGraphsDBHelper(this);

    private Sensors mSensors = new Sensors();
    private StringBuilders mStringBuilder = new StringBuilders();

    public void onCreate() {
        super.onCreate();
        sensorManagerService = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManagerService != null) {
            sensorGyroscope = sensorManagerService.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorAccelerometer = sensorManagerService.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }

        notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (sensorManagerService != null) {
            sensorManagerService.registerListener(listener, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManagerService.registerListener(listener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            dataProcessing();
            cleaningOfDatabases();
        }

        String textFoNotificationUp = getResources().getString(R.string.stTextForNotification) + ":";
        String textFoNotificationDown = " '"
                + getResources().getString(R.string.stAcceleration) + "' и '"
                + getResources().getString(R.string.stGyroscope) + "'";
        sendNotification(textFoNotificationUp, textFoNotificationDown);

        return START_REDELIVER_INTENT;
    }

    public void onDestroy() {

        if (sensorManagerService != null) {
            sensorManagerService.unregisterListener(listener);
        }
        scheduleDataProcessing.shutdownNow();
        scheduleCleaningOfDatabases.shutdownNow();

        mDbHelperGraphs.Close_DB_for_graphs();
        mDbHelperGraphs.close();

        notificationManager.cancel(iDefaultNotificationId);

        stopSelf();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }


    private SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            float[] valuesGyroscope = new float[3];
            float[] valuesAcceleration = new float[3];
            String stfinishedLine;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    System.arraycopy(event.values, 0, valuesGyroscope, 0, 3);
                    stfinishedLine = mStringBuilder.bsDataFromSensors(valuesGyroscope);
                    arrValuesGyroscope.add(stfinishedLine);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    System.arraycopy(event.values, 0, valuesAcceleration, 0, 3);
                    stfinishedLine = mStringBuilder.bsDataFromSensors(valuesAcceleration);
                    arrValuesAcceleration.add(stfinishedLine);
                    break;
            }
        }
    };

    /*
        Каждую секунду мы берем массивы значений с датчиков и отправляем на обработку.
     */
    private void dataProcessing() {

        scheduleDataProcessing = Executors.newScheduledThreadPool(1);
        scheduleDataProcessing.scheduleAtFixedRate(new Runnable() {
            public void run() {
                acquireWakeLock();

                ArrayList<String> arBuffer1 = new ArrayList<>(arrValuesGyroscope);
                ArrayList<String> arBuffer2 = new ArrayList<>(arrValuesAcceleration);
                arrValuesGyroscope.clear();
                arrValuesAcceleration.clear();
                processingOfDataFromSensors(arBuffer1, arBuffer2);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /*
        Массив данных, полученный от датчиков за секунду, мы отправляем на обработку.
        Её результатом является строка, которая содержит среднее значения по трем осям датчиков
        Эта строка записывается в БД которая хранит данные для построения графиков
     */
    private void processingOfDataFromSensors(ArrayList<String> arbBuffer1, ArrayList<String> arBuffer2) {

        long lTimeNow = takeTimeNow();
        String stRowFotWriting;

        /*
            Если датчика нет в устройстве, то на вход будем получать массив 0'вого размера.
            А значит нет данных для обработки. Следовательно обработка и запись в БД не требуется.
         */
        if (arbBuffer1.size() > 0){
            String stSensorType = getResources().getString(R.string.stGyroscope);
            stRowFotWriting = mSensors.gyroscope(arbBuffer1); //отправиили массив, получили строку
            mDbHelperGraphs.recordingDataForGraphs(stSensorType, lTimeNow, stRowFotWriting); //записали строку в БД
        }

        if(arBuffer2.size() > 0){
            String stSensorType = getResources().getString(R.string.stAcceleration);
            stRowFotWriting = mSensors.acceleration(arBuffer2);
            mDbHelperGraphs.recordingDataForGraphs(stSensorType, lTimeNow, stRowFotWriting);
        }
    }

    /*
        Дабы на захламлять базу, каждую минуту удаляем записи свыше 6 минут
     */
    private void cleaningOfDatabases(){

        scheduleCleaningOfDatabases = Executors.newScheduledThreadPool(1);
        scheduleCleaningOfDatabases.scheduleAtFixedRate(new Runnable() {
            public void run() {
                cleanerDB();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    //метод отчистки базы
    private void cleanerDB(){
        long lTimeNow = takeTimeNow();
        String stIntervalStep = getResources().getString(R.string.stIntervalStep);
        int iIntervalStepForGraphs = Integer.parseInt(stIntervalStep) + 60000; //в миллисекундах. За какой промежуток будем делать удаление
        mDbHelperGraphs.clearDBForGraphs(lTimeNow, iIntervalStepForGraphs);
    }

    //метод получения времени в данный момент
    private long takeTimeNow(){
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

    //создаем уведомление
    private void sendNotification(String stTitle, String stText) {

        String stChannelId = getResources().getString(R.string.stChannelName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {


            CharSequence chName = getResources().getString(R.string.stChannelName);
            String stDescription = getResources().getString(R.string.stChannelDescription);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(stChannelId, chName, importance);
            mChannel.setDescription(stDescription);
            notificationManager.createNotificationChannel(mChannel);

        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, stChannelId);
        builder.setContentIntent(contentIntent)
                .setOngoing(true)   //Запретить "смахивание"
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(stTitle) //Заголовок
                .setContentText(stText) // Текст уведомления
                .setWhen(System.currentTimeMillis());

        Notification notification;
        notification = builder.build();

        startForeground(iDefaultNotificationId, notification);
    }

    //метод не позволяющий телефону уходить в режим сна
    private void acquireWakeLock() {
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        releaseWakeLock();
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PARTIAL_WAKE_LOCK");
            mWakeLock.acquire(60*1000L /*1 minute*/);
        }

    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
