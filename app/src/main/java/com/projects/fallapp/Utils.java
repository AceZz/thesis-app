package com.projects.fallapp;

public class Utils {
    public static int argMax(float[] a, int start, int end) {
        float v = Float.MIN_VALUE;
        int ind = -1;
        for (int i = start; i < end; i++) {
            if (a[i] > v) {
                v = a[i];
                ind = i;
            }
        }
        return ind;
    }

    public static float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float)arr[i];
        }
        return ret;
    }

    public static double[] toDoubleArray(float[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        double[] ret = new double[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (double)arr[i];
        }
        return ret;
    }

    public static double[] subtractArray(double[] arr1, double[] arr2) {
        int n1 = arr1.length;
        int n2 = arr2.length;
        assert n1 == n2;
        double ret[] = new double[n1];
        for (int i = 0; i < n1; i++) {
            ret[i] = arr1[i] - arr2[i];
        }
        return ret;
    }

    public static double[] concatenate(double[] arr1, double[] arr2) {
        int n1 = arr1.length;
        int n2 = arr2.length;
        double ret[] = new double[n1+n2];
        for (int i = 0; i < n1; i++) {
            ret[i] = arr1[i];
        }
        for (int i = n1; i < n1+n2; i++) {
            ret[i] = arr2[i-n1];
        }
        return ret;
    }

    public static float[][] shiftLeftArray(float[][] arr, int shiftLength) {
        int n = arr.length;
        float[][] ret = new float[n][3];
        for (int i = 0; i < n - shiftLength; i++) {
            for (int j = 0; j < 3; j++){
                ret[i][j] = arr[i+shiftLength][j];
            }
        }
        return ret;
    }

    public static float[] normalizeFlattenArray(float[] arr, int streamLength) {
        int n = arr.length;
        assert 3 * streamLength == n;
        float[] ret = new float[n];
        for (int j = 0; j < 3; j++){
            float max = arr[j*streamLength];
            float min = arr[j*streamLength];
            for (int i = 0; i < streamLength; i++) {
                if (arr[j*streamLength+i] > max) {
                    max = arr[j*streamLength+i];
                }
                if (arr[j*streamLength+i] < min) {
                    min = arr[j*streamLength+i];
                }
            }
            for (int i = 0; i < streamLength; i++) {
                ret[j*streamLength+i] = (float) ((arr[j*streamLength+i] - min) / (1e-5 + max - min));
            }
        }
        return ret;
    }
}
