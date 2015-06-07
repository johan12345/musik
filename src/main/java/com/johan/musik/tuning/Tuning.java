package com.johan.musik.tuning;

public interface Tuning {
    public static final int C = 60;
    public static final int A = 69;
    public static final double A_FREQ = 440;

    public double getFrequency(int tone);
}
