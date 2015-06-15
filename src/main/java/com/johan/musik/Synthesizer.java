package com.johan.musik;

import com.johan.musik.tuning.Tuning;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.*;

public class Synthesizer extends MidiReceiver {
    public static final float SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 2;
    private static final int BUFFER_SIZE = 4400;
    private static final short AMPLITUDE = 1000;
    private final Map<Integer, Tone> tones = Collections.synchronizedMap(new HashMap<Integer, Tone>());
    private final AudioThread thread = new AudioThread();
    private Tuning tuning;
    private SourceDataLine line;
    private boolean stop = false;
    private boolean sustain = false;
    private double[] overtoneAmplitudes;
    private double amplitudeCalibration;
    private double overtoneExpansion;
    private double lastToneFreq;
    private List<AudioThreadListener> listeners = new ArrayList<>();

    private double a;
    private double d;
    private double s;
    private double r;

    public Synthesizer(double overtoneExpansion, Tuning tuning, double[] overtoneAmplitudes) throws LineUnavailableException {
        super();
        this.overtoneExpansion = overtoneExpansion;
        this.tuning = tuning;
        setAdsr(800, 10, 0.5, 100);
        setOvertoneAmplitudes(overtoneAmplitudes);
        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                8 * SAMPLE_SIZE,  // sample size in bits
                1,  // channels
                true,  // signed
                true  // bigendian
        );
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format, BUFFER_SIZE);
        line.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                System.out.println(event.getType().toString());
            }
        });
        line.start();
        thread.start();
    }

    @Override
    protected void startTone(int toneNum) {
        Tone tone = new Tone();
        tone.envelopeProvider = new AdsrEnvelopeProvider(a, d, s, r, AMPLITUDE);
        if (sustain) tone.envelopeProvider.startSustain();
        tones.put(toneNum, tone);

        double cents = 1200 * Math.log(tuning.getFrequency(toneNum) / lastToneFreq) / Math.log(2);
        System.out.println(cents + " Cents");
        lastToneFreq = tuning.getFrequency(toneNum);
    }

    @Override
    protected void endTone(int tone) {
        if (tones.containsKey(tone)) tones.get(tone).envelopeProvider.releaseTone();
    }

    @Override
    protected void setSustain(int sustain) {
        boolean newSustain = sustain >= 67;
        if (this.sustain != newSustain) {
            synchronized (tones) {
                for (Tone tone : tones.values()) {
                    if (newSustain) {
                        tone.envelopeProvider.startSustain();
                    } else {
                        tone.envelopeProvider.stopSustain();
                    }
                }
            }
        }
        this.sustain = newSustain;
    }

    public void setTuning(Tuning tuning) {
        this.tuning = tuning;
    }

    public void setOvertoneExpansion(double overtoneExpansion) {
        this.overtoneExpansion = overtoneExpansion;
    }

    private double[] getOvertoneFreqs(double freq, int count) {
        double[] overtones = new double[count];
        for (int i = 2; i <= count + 1; i++) {
            overtones[i - 2] = freq * i * Math.pow(overtoneExpansion, ((double) i) / 2);
        }
        return overtones;
    }

    public Map<Double, Double> getAllTones() {
        Map<Double, Double> allTones = new HashMap<>();
        for (Map.Entry<Integer, Tone> entry : tones.entrySet()) {
            Tone tone = entry.getValue();
            double freq = tuning.getFrequency(entry.getKey());
            double amplitude = tone.envelopeProvider.getEnvelopeValue();
            if (!allTones.containsKey(freq)) {
                allTones.put(freq, amplitude);
            } else {
                allTones.put(freq, allTones.get(freq) + amplitude);
            }
            double[] overtoneFreqs = getOvertoneFreqs(freq, overtoneAmplitudes.length - 1);
            for (int j = 1; j < overtoneAmplitudes.length; j++) {
                if (!allTones.containsKey(overtoneFreqs[j - 1])) {
                    allTones.put(overtoneFreqs[j - 1], overtoneAmplitudes[j] * amplitude);
                } else {
                    allTones.put(overtoneFreqs[j - 1], allTones.get(overtoneFreqs[j - 1]) + overtoneAmplitudes[j] * amplitude);
                }
            }
        }
        return allTones;
    }

    @Override
    public void close() {
        stop = true;
    }

    public void addAudioThreadListener(AudioThreadListener listener) {
        listeners.add(listener);
    }

    public void removeAudioThreadListener(AudioThreadListener listener) {
        listeners.remove(listener);
    }

    public void setOvertoneAmplitudes(double[] overtoneAmplitudes) {
        this.overtoneAmplitudes = overtoneAmplitudes;
        int sum = 0;
        for (double amplitude : overtoneAmplitudes) {
            sum += Math.pow(amplitude, 2);
        }
        amplitudeCalibration = Math.sqrt(sum);
    }

    public void setAdsr(double a, double d, double s, double r) {
        this.a = a;
        this.d = d;
        this.s = s;
        this.r = r;
    }

    public interface AudioThreadListener {
        void onNewSamples(double time, short[] data);
    }

    private class AudioThread extends Thread {
        private double time = 0;

        @Override
        public void run() {
            ByteBuffer buf = ByteBuffer.allocate(line.getBufferSize());
            ShortBuffer shortBuf = ShortBuffer.allocate(line.getBufferSize());
            while (!stop) {
                try {
                    buf.clear();
                    shortBuf.clear();
                    int samplesThisPass = line.available() / SAMPLE_SIZE;
                    for (int i = 0; i < samplesThisPass; i++) {
                        short data = 0;
                        synchronized (tones) {
                            Iterator<Map.Entry<Integer, Tone>> iterator = tones.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<Integer, Tone> entry = iterator.next();
                                Tone tone = entry.getValue();
                                double freq = tuning.getFrequency(entry.getKey());
                                double amplitude = tone.envelopeProvider.getEnvelopeValue() /
                                        amplitudeCalibration;
                                if (amplitude == 0) iterator.remove();
                                data += overtoneAmplitudes[0] * amplitude * Math
                                        .sin(2 * Math.PI * time * freq);
                                double[] overtoneFreqs = getOvertoneFreqs(freq, overtoneAmplitudes.length - 1);
                                for (int j = 1; j < overtoneAmplitudes.length; j++) {
                                    data += overtoneAmplitudes[j] * amplitude * Math
                                            .sin(2 * Math.PI * time * overtoneFreqs[j - 1]);
                                }
                            }
                        }

                        buf.putShort(data);
                        shortBuf.put(data);
                        time += 1. / SAMPLE_RATE;
                    }
                    line.write(buf.array(), 0, buf.position());
                    for (AudioThreadListener listener : listeners) {
                        listener.onNewSamples(time, Arrays.copyOfRange(shortBuf.array(), 0, shortBuf.position()));
                    }

                    //Wait until the buffer is at least half empty  before we add more
                    /*while (line.getBufferSize() / 2 < line.available()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
