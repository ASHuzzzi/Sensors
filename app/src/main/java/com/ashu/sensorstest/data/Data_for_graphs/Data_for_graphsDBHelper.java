package com.ashu.sensorstest.data.Data_for_graphs;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.ArrayMap;

import com.ashu.sensorstest.data.Data_for_graphs.Data_for_graphsDBContract.DBDataCollection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class Data_for_graphsDBHelper extends SQLiteOpenHelper {
    // путь к базе данных вашего приложения
    @SuppressLint("SdCardPath")
    private final String DB_PATH = "/data/data/com.ashu.sensorstest/databases/";
    private static String DB_NAME = "Data_for_graphs.db";
    private SQLiteDatabase myDataBase;
    private final Context mContext;

    public Data_for_graphsDBHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.mContext = context;
    }

    /**
     * Создает пустую базу данных и перезаписывает ее нашей собственной базой
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
            String myPath = DB_PATH + DB_NAME;
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
        //Открываем локальную БД как входящий поток
        InputStream myInput = mContext.getAssets().open("db/" + DB_NAME);

        //Путь ко вновь созданной БД
        String outFileName = DB_PATH + DB_NAME;
        //String outFileName = DB_PATH;

        //Открываем пустую базу данных как исходящий поток
        OutputStream myOutput = new FileOutputStream(outFileName);

        //перемещаем байты из входящего файла в исходящий
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //закрываем потоки
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public void openDataBase() throws SQLException {
        //открываем БД
        String myPath = DB_PATH + DB_NAME;
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

    //запись в базу данных с датчиков
    public void Recording_Data_for_graphs(String SensorType, long TimeOfChange, String SensorData){
        myDataBase = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        newValues.put(DBDataCollection.Column_SensorType, SensorType);
        newValues.put(DBDataCollection.Column_DataTime, TimeOfChange);
        newValues.put(DBDataCollection.Column_SensorData, SensorData);
        myDataBase.insert(DBDataCollection.TABLE_NAME, null, newValues);
    }

    public void Clear_DB_for_graphs(long TimeOfChange, int Interval_Step){ //чтобы не захламлять БД, чистим ее каждую минуту от данных страше двух минут с момента запуска
        myDataBase = this.getWritableDatabase();

        String selection = DBDataCollection.Column_DataTime + " < " + (TimeOfChange - Interval_Step);

        myDataBase.delete(
                DBDataCollection.TABLE_NAME,
                selection,
                null
        );
    }

    public ArrayMap<Long, String> Read_DBData_for_graphs(String SensorType, long TimeOfChange, int Interval_Step){
        myDataBase = this.getReadableDatabase();

        Cursor cursor;
        ArrayMap<Long, String> Query_Result = new ArrayMap<>();
        long begin_selection = TimeOfChange - Interval_Step;

        String[] projection = {
                DBDataCollection.Column_DataTime,
                DBDataCollection.Column_SensorData};

        String selection = DBDataCollection.Column_SensorType + "= '" + SensorType + "' AND "
                + DBDataCollection.Column_DataTime + " BETWEEN " + (begin_selection) + " AND "
                + (TimeOfChange);

        cursor = myDataBase.query(
                DBDataCollection.TABLE_NAME,  // таблица
                projection,            // столбцы
                selection,             // столбцы для условия WHERE
                null,         // значения для условия WHERE
                null,                  // Don't group the rows
                null,                  // Don't filter by row groups
                null                   // порядок сортировки
        );

        if (cursor !=null && cursor.moveToFirst()){
            do {
                Query_Result.put(
                        cursor.getLong(cursor.getColumnIndex(DBDataCollection.Column_DataTime)),
                        cursor.getString(cursor.getColumnIndex(DBDataCollection.Column_SensorData)));
            }while (cursor.moveToNext());

        }
        Objects.requireNonNull(cursor).close();
        return Query_Result;
    }

    public void Close_DB_for_graphs(){
        myDataBase.close();
    }
}
