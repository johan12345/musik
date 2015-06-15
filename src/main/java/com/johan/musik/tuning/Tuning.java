package com.johan.musik.tuning;

public interface Tuning {
    int C = 60;
    int A = 69;
    double A_FREQ = 440;

    double getFrequency(int tone);
}
