package com.ashu.sensorstest.string_builders;

/*
*   Класс служит для преобразования входящих массивов в строки
* с тем форматом, который подходит для записи и дальнейшей работы.
* Методы вынесены в этот класс на случай, если мы захотим поменять
* структуру хранимых данных
 */

import android.annotation.SuppressLint;

import java.util.ArrayList;

public class StringBuilders {

    private StringBuilder stringBuilder; //для преобразования данных в строку, для записи в БД
    private final String delimiter = ";"; //чтобы в обеих базах знак разделителя был один.

    //работает с данными, полученными от датчиков сразу
    public String bsDataFromSensors(float[] arfSensorData){
        stringBuilder = new StringBuilder();
        for (float aSensor_data : arfSensorData) {
            @SuppressLint("DefaultLocale") String converted_data = String.format("%1$.1f", aSensor_data);
            stringBuilder.append(converted_data).append(delimiter);
        }
        return String.valueOf(stringBuilder);
    }

    // готовит данные для записи в БД, которая потом используется для построения графиков
    public String bsDataForGraphs(ArrayList<Double> arrResultOfWork){
        stringBuilder = new StringBuilder();
        for (int i = 0; i < arrResultOfWork.size(); i++){
            stringBuilder.append(arrResultOfWork.get(i)).append(delimiter);
        }
        return String.valueOf(stringBuilder);
    }
}
