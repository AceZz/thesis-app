package com.projects.firstapptutorial;

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
}
