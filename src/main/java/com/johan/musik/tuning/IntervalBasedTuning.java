package com.johan.musik.tuning;

public abstract class IntervalBasedTuning implements Tuning {
    private int baseTone;
    private double baseFreq;

    public IntervalBasedTuning(int baseTone) {
        this.baseTone = baseTone;
        if (baseTone == A) {
            baseFreq = A_FREQ;
        } else {
            this.baseFreq = new PythagoreanTuning(A).getFrequency(baseTone);
        }
    }

    @Override
    public double getFrequency(int tone) {
        int current = baseTone;
        double currentFreq = baseFreq;
        while (current != tone) {
            int difference = tone - current;
            if (difference >= 12) {
                current += 12;
                currentFreq *= getInterval(12);
            } else if (difference < 0) {
                current -= 12;
                currentFreq /= getInterval(12);
            } else {
                for (int i = 0; i <= 11; i++) {
                    if (difference == i) {
                        current += i;
                        currentFreq *= getInterval(i);
                        break;
                    }
                }
            }
        }
        return currentFreq;
    }

    abstract double getInterval(int number);
}
