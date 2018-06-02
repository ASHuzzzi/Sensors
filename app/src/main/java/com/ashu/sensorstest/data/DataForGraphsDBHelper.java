package com.ashu.sensorstest.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.ashu.sensorstest.data.DataForGraphsDBContract.DBDataCollection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class DataForGraphsDBHelper extends SQLiteOpenHelper {
    @SuppressLint("SdCardPath")
    private final String stDBPath = "/data/data/com.ashu.sensorstest/databases/";
    private static String stDBName = "Data_for_graphs.db";
    private SQLiteDatabase myDataBase;
    private final Context mContext;

    public DataForGraphsDBHelper(Context context) {
        super(context, stDBName, null, 1);
        this.mContext = context;
    }

    /**
     * Создает пустую базу данных и перезаписывает ее моей базой
     * */
    public void createDataBase() {

        if(!checkDataBase()){
            //вызывая этот метод создаем пустую базу, позже она будет перезаписана
            this.getReadableDatabase();

            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /**
     * Проверяет, существует ли уже эта база, чтобы не копировать каждый раз при запуске приложения
     * @return true если существует, false если не существует
     */
    private boolean checkDataBase(){
        SQLiteDatabase checkDB = null;

        try{
            String myPath = stDBPath + stDBName;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        }catch(SQLiteException e){
            //база еще не существует
        }
        if(checkDB != null){
            checkDB.close();
        }
        return checkDB != null;
    }

    /**
     * Копирует базу из папки assets заместо созданной локальной БД
     * Выполняется путем копирования потока байтов.
     * */
    private void copyDataBase() throws IOException{
        InputStream myInput = mContext.getAssets().open("db/" + stDBName);

        String outFileName = stDBPath + stDBName;
        OutputStream myOutput = new FileOutputStream(outFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public void openDataBase() throws SQLException {
        //открываем БД
        String myPath = stDBPath + stDBName;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close() {
        if(myDataBase != null)
            myDataBase.close();
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    /*
        Запись в базу данных с датчиков
        На вход принимает тип датчика, время изменения, данные для этого периода времени
     */
    public void recordingDataForGraphs(String stSensorType, long lTimeOfChange, String stSensorData){
        myDataBase = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        newValues.put(DBDataCollection.stColumnSensorType, stSensorType);
        newValues.put(DBDataCollection.stColumnDataTime, lTimeOfChange);
        newValues.put(DBDataCollection.stColumnSensorData, stSensorData);
        myDataBase.insert(DBDataCollection.stTableName, null, newValues);
    }

    //чтобы не захламлять БД, чистим ее каждую минуту от данных страше шесть минут с момента запуска
    public void clearDBForGraphs(long lTimeOfChange, int iIntervalStep){
        myDataBase = this.getWritableDatabase();

        String selection = DBDataCollection.stColumnDataTime + " < " + (lTimeOfChange - iIntervalStep);

        myDataBase.delete(
                DBDataCollection.stTableName,
                selection,
                null
        );
    }

    /*
        Чтение данных из базы
        На вход принимает тип датчика, время начала выборки, величину периода за который будет
        делаться выборка
     */
    public ArrayList<String> readDBDataForGraphs(String stSensorType, long lTimeOfChange, int iInterval_Step){
        myDataBase = this.getReadableDatabase();

        Cursor cursor;
        ArrayList<String> arQueryResult = new ArrayList<>();
        long lBeginSelection = lTimeOfChange - iInterval_Step;

        String[] projection = {DBDataCollection.stColumnSensorData};

        String selection = DBDataCollection.stColumnSensorType + "= '" + stSensorType + "' AND "
                + DBDataCollection.stColumnDataTime + " BETWEEN " + (lBeginSelection) + " AND "
                + (lTimeOfChange);

        cursor = myDataBase.query(
                DBDataCollection.stTableName,
                projection,
                selection,
                null,
                null,
                null,
                null
        );

        if (cursor !=null && cursor.moveToFirst()){
            do {
                arQueryResult.add(
                        cursor.getString(cursor.getColumnIndex(DBDataCollection.stColumnSensorData)));
            }while (cursor.moveToNext());
            cursor.close();
        }
        return arQueryResult;
    }

    public void Close_DB_for_graphs(){
        myDataBase.close();
    }

}
