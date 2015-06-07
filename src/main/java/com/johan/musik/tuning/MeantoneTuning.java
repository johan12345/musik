package com.johan.musik.tuning;

/**
 * 1/4 Comma Meantone Tuning
 * Source: http://de.wikipedia.org/wiki/Mitteltönige_Stimmung#Aufbau_der_Tonleiter
 */
public class MeantoneTuning extends IntervalBasedTuning implements Tuning {
    private static final double[] INTERVALS = new double[]{
            1.,              // Prime
            5. / 16 * Math.pow(5, 3. / 4),      // Kleine Sekunde
            1. / 2 * Math.sqrt(5),          // Große Sekunde
            4. / 5 * Math.pow(5, 1. / 4),        // kleine Terz
            5. / 4,        // große Terz
            2. / 5 * Math.pow(5, 3. / 4),          // Quarte
            5. / 8 * Math.sqrt(5),      // Tritonus
            Math.pow(5, 1. / 4),          // Quinte
            25. / 16,       // kleine Sexte
            1. / 2 * Math.pow(5, 3. / 4),        // große Sexte
            4. / 5 * Math.sqrt(5),         // kleine Septime
            5. / 4 * Math.pow(5, 1. / 4),      // große Septime
            2.               // Oktave
    };

    public MeantoneTuning(int baseTone) {
        super(baseTone);
    }


    @Override
    double getInterval(int number) {
        return INTERVALS[number];
    }
}
