package com.johan.musik.tuning;

/**
 * Pythagorean Tuning
 * Source: http://en.wikipedia.org/wiki/Pythagorean_tuning#Method
 */
public class PythagoreanTuning extends IntervalBasedTuning implements Tuning {
    private static final double[] INTERVALS = new double[]{
            1.,              // Prime
            256. / 243,      // Kleine Sekunde
            9. / 8,          // Große Sekunde
            32. / 27,        // kleine Terz
            81. / 64,        // große Terz
            4. / 3,          // Quarte
            729. / 512,      // Tritonus
            3. / 2,          // Quinte
            128. / 81,       // kleine Sexte
            27. / 16,        // große Sexte
            16. / 9,         // kleine Septime
            243. / 128,      // große Septime
            2.               // Oktave
    };

    public PythagoreanTuning(int baseTone) {
        super(baseTone);
    }


    @Override
    double getInterval(int number) {
        return INTERVALS[number];
    }
}
