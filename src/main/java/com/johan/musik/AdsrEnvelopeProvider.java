package com.johan.musik;

public class AdsrEnvelopeProvider implements EnvelopeProvider {
    private AdsrPhase phase = AdsrPhase.ATTACK;
    private double a, d, s, r;
    private boolean shouldRelease;
    private boolean toneReleased;
    private boolean sustain = false;
    private double maxAmplitude;
    private double amplitude = 0;

    public AdsrEnvelopeProvider(double a, double d, double s, double r, double maxAmplitude) {
        this.a = a * maxAmplitude;
        this.d = d * maxAmplitude;
        this.s = s * maxAmplitude;
        this.r = r * maxAmplitude;
        this.maxAmplitude = maxAmplitude;
    }

    @Override
    public void startSustain() {
        sustain = true;
    }

    @Override
    public void stopSustain() {
        sustain = false;
        if (toneReleased) shouldRelease = true;
    }

    @Override
    public void releaseTone() {
        toneReleased = true;
        if (!sustain) shouldRelease = true;
    }

    @Override
    public double getEnvelopeValue() {
        switch (phase) {
            case ATTACK:
                if (amplitude >= maxAmplitude) {
                    phase = AdsrPhase.DECAY;
                } else {
                    amplitude += a / Synthesizer.SAMPLE_RATE;
                }
                break;
            case DECAY:
                if (amplitude <= s) {
                    phase = AdsrPhase.SUSTAIN;
                } else {
                    amplitude -= d / Synthesizer.SAMPLE_RATE;
                }
                break;
            case SUSTAIN:
                if (shouldRelease) {
                    phase = AdsrPhase.RELEASE;
                } else {
                    amplitude = s;
                }
                break;
            case RELEASE:
                amplitude -= r / Synthesizer.SAMPLE_RATE;
                if (amplitude < 0) {
                    amplitude = 0;
                }
                break;
        }
        return amplitude;
    }

    private enum AdsrPhase {
        ATTACK, DECAY, SUSTAIN, RELEASE
    }
}
