package com.johan.musik;

public interface EnvelopeProvider {
    public void startSustain();

    public void stopSustain();

    public void releaseTone();

    public double getEnvelopeValue();
}
