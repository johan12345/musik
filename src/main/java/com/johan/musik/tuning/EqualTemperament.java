package com.johan.musik.tuning;

public class EqualTemperament implements Tuning {
    public static final double FACTOR = Math.pow(2, 1. / 12);
    private double factor;

    public EqualTemperament() {
        factor = FACTOR;
    }

    public EqualTemperament(double expansion) {
        factor = Math.pow(2 * expansion, 1. / 12);
    }

    @Override
    public double getFrequency(int tone) {
        int difference = tone - A;
        return A_FREQ * Math.pow(factor, difference);
    }
}
