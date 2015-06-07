package com.johan.musik.tuning;

/**
 * Meantone Tuning
 * Source: http://www.phy.mtu.edu/~suits/scales.html
 */
public class JustTuning extends IntervalBasedTuning implements Tuning {
    private static final double[] INTERVALS = new double[]{
            1.,              // Prime
            25. / 24,      // Kleine Sekunde
            9. / 8,          // Große Sekunde
            6. / 5,        // kleine Terz
            5. / 4,        // große Terz
            4. / 3,          // Quarte
            45. / 32,      // Tritonus
            3. / 2,          // Quinte
            8. / 5,       // kleine Sexte
            5. / 3,        // große Sexte
            9. / 5,         // kleine Septime
            15. / 8,      // große Septime
            2.               // Oktave
    };

    public JustTuning(int baseTone) {
        super(baseTone);
    }


    @Override
    double getInterval(int number) {
        return INTERVALS[number];
    }
}
