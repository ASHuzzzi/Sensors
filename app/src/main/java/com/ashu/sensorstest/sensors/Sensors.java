package com.ashu.sensorstest.sensors;

import com.ashu.sensorstest.string_builders.StringBuilders;

import java.util.ArrayList;


/*
В данном классе обрабатываются данные с сенсоров за минуту. На входе имеем массив, на
выходе среднее значение.
Методы для акселерометра и гироскопа идентичны, т.к. они дают на выходе 3 переменные и
их можно было бы свести к одному и сделать более универсальным. Но решил оставить, дабы показать, что:
1) обработку данных внутри метода для каждого датчика можно настроить как угодно, и это не повлияет
на обработку данных для других датчиков;
2) можно добавить/удалить любое количество других датчиков, и это также не повлияет на работу
остальных.
 */
public class Sensors {

    private double[] x_value;
    private double[] y_value;
    private double[] z_value;
    private ArrayList<Double> result_of_work;
    private StringBuilders stringBuilder = new StringBuilders();


    static {
        System.loadLibrary("native-lib");
    }

    public native double CountAVR(double[] x_value);


    /*
        На вход получаем массив строк.
        Из кадой строки получаем XYZ. Их записываем в соответстувющие массивы.
        Полученные массивы отправляем на обработку С++ методом.
        На выходе после каждой обработки имеем 1 одно среднее значение для соотвествующей оси.
        Три полученных значения собираем в строку.
     */
    public String Acceleration(ArrayList<String> arr_value){
        x_value = new double[arr_value.size()];
        y_value = new double[arr_value.size()];
        z_value = new double[arr_value.size()];
        result_of_work = new ArrayList<>();
        for (int i = 0; i<arr_value.size(); i++){
            String[] separated = new String[3];
            String[] shared_request = arr_value.get(i).split(";");
            for (int j = 0; j < 3; j++){

                separated[j] = shared_request[j].replace(",", ".");

            }
            x_value[i] = Double.parseDouble(separated[0]);
            y_value[i] = Double.parseDouble(separated[1]);
            z_value[i] = Double.parseDouble(separated[2]);

        }
        if(Double.isNaN(CountAVR(x_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(x_value));
        }
        if(Double.isNaN(CountAVR(y_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(y_value));
        }
        if(Double.isNaN(CountAVR(z_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(z_value));
        }

        return stringBuilder.BS_Data_for_graphs(result_of_work);
    }

    public String Gyroscope (ArrayList<String> arr_value){
        x_value = new double[arr_value.size()];
        y_value = new double[arr_value.size()];
        z_value = new double[arr_value.size()];
        result_of_work = new ArrayList<>();
        for (int i = 0; i<arr_value.size(); i++){
            String[] separated = new String[3];
            String[] shared_request = arr_value.get(i).split(";");
            for (int j = 0; j < 3; j++){

                separated[j] = shared_request[j].replace(",", ".");

            }
            x_value[i] = Double.parseDouble(separated[0]);
            y_value[i] = Double.parseDouble(separated[1]);
            z_value[i] = Double.parseDouble(separated[2]);

        }
        if(Double.isNaN(CountAVR(x_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(x_value));
        }
        if(Double.isNaN(CountAVR(y_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(y_value));
        }
        if(Double.isNaN(CountAVR(z_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(z_value));
        }

        return stringBuilder.BS_Data_for_graphs(result_of_work);
    }
}
