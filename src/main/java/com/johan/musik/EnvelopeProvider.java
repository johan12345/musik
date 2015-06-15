package com.johan.musik;

public interface EnvelopeProvider {
    void startSustain();

    void stopSustain();

    void releaseTone();

    double getEnvelopeValue();
}
