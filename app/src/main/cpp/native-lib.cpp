#include <jni.h>

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_ashu_sensorstest_sensors_Sensors_CountAVR(JNIEnv *env, jobject instance,
                                                   jdoubleArray arr_value) {

    jdouble *xr; // указатель для выделения памяти под массив
    jsize size;// размер массива
    size = (*env).GetArrayLength(arr_value);
    xr = (*env).GetDoubleArrayElements(arr_value, 0); // выделение памяти под массив

    double max = xr[0];
    double min = xr[0];
    double sum=xr[0];
    for (int i = 1; i < size; i++) {
        if (xr[i] > max) max = xr[i];
        if (xr[i] < min) min = xr[i];
        sum += xr[i];
    }

    double average = (sum - max - min) / (size - 2);

    delete [] xr; // освобождение памяти

    return average;
}