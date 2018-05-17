package com.ashu.sensorstest.data.Data_from_sensors;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.ashu.sensorstest.data.Data_from_sensors.Data_from_sensorsDBContract.DBDataCollection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Data_from_sensorsDBHelper extends SQLiteOpenHelper {

    // путь к базе данных вашего приложения
    @SuppressLint("SdCardPath")
    private static String DB_PATH = "/data/data/com.ashu.sensorstest/databases/";
    private static String DB_NAME = "Data_from_sensors.db";
    private SQLiteDatabase myDataBase;
    private final Context mContext;

    public Data_from_sensorsDBHelper(Context context) {
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
    public void Recording_Data_From_Sensors(String SensorType, long TimeOfChange, String SensorData){
        myDataBase = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        newValues.put(DBDataCollection.Column_SensorType, SensorType);
        newValues.put(DBDataCollection.Column_DataTime, TimeOfChange);
        newValues.put(DBDataCollection.Column_SensorData, SensorData);
        myDataBase.insert(DBDataCollection.TABLE_NAME, null, newValues);

    }

    // чтение данных из базы
    public ArrayList<String> Read_DBData_From_Sensors(String SensorType, long TimeOfChange, int Interval_Step){
        myDataBase = this.getReadableDatabase();

        Cursor cursor;
        ArrayList<String> Query_Result = new ArrayList<>();

        String[] projection = {DBDataCollection.Column_SensorData};

        String selection = DBDataCollection.Column_SensorType + "= '" + SensorType + "' AND "
                + DBDataCollection.Column_DataTime + " BETWEEN " + (TimeOfChange- Interval_Step) + " AND "
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
                Query_Result.add(cursor.getString(cursor.getColumnIndex(DBDataCollection.Column_SensorData)));
            }while (cursor.moveToNext());
            cursor.close();
        }

        return Query_Result;
    }

    public void Clear_DB_for_sensors(long TimeOfChange, int Interval_Step){ //чтобы не захламлять БД, чистим ее каждую минуту от данных страше двух минут с момента запуска
        myDataBase = this.getWritableDatabase();

        String selection = DBDataCollection.Column_DataTime + " < " + (TimeOfChange - Interval_Step);

        myDataBase.delete(
                DBDataCollection.TABLE_NAME,
                selection,
                null
        );
    }

    public void Close_DB_from_sensors(){
        myDataBase.close();
    }

    // Здесь можно добавить вспомогательные методы для доступа и получения данных из БД
    // вы можете возвращать курсоры через "return myDataBase.query(....)", это облегчит их использование
// в создании адаптеров для ваших view
}
