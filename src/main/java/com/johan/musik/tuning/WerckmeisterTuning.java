package com.johan.musik.tuning;

/**
 * Werckmeister I Tuning ("Wohltemperierte Stimmung")
 * Source: http://en.wikipedia.org/wiki/Werckmeister_temperament#Werckmeister_I_.28III.29:_.22correct_temperament.22_based_on_1.2F4_comma_divisions
 */
public class WerckmeisterTuning extends IntervalBasedTuning implements Tuning {
    private static final double[] INTERVALS = new double[]{
            1.,              // Prime
            256. / 243,      // Kleine Sekunde
            64. / 81 * Math.sqrt(2),          // Große Sekunde
            32. / 27,        // kleine Terz
            256. / 243 * Math.pow(2, 1. / 4),        // große Terz
            4. / 3,          // Quarte
            1024. / 729,      // Tritonus
            8. / 9 * Math.pow(2, 3. / 4),          // Quinte
            128. / 81,       // kleine Sexte
            1024. / 729 * Math.pow(2, 1. / 4),        // große Sexte
            16. / 9,         // kleine Septime
            128. / 81 * Math.pow(2, 1. / 4),      // große Septime
            2.               // Oktave
    };

    public WerckmeisterTuning(int baseTone) {
        super(baseTone);
    }


    @Override
    double getInterval(int number) {
        return INTERVALS[number];
    }
}
