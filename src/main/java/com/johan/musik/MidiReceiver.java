package com.johan.musik;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public abstract class MidiReceiver implements Receiver {

    public MidiReceiver() {
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage msg = (ShortMessage) message;
            if (msg.getStatus() == 144 && msg.getData2() > 0) {
                startTone(msg.getData1());
            } else if (msg.getStatus() == 128 || (msg.getStatus() == 144 && msg.getData2() == 0)) {
                endTone(msg.getData1());
            } else if (msg.getStatus() == 176) {
                if (msg.getData1() == 64) {
                    setSustain(msg.getData2());
                }
            } else if (message.getStatus() != 248 && message.getStatus() != 254) {
                System.out.println("Message " + msg.getStatus());
            }

        }
    }

    protected abstract void startTone(int tone);

    protected abstract void endTone(int tone);

    protected abstract void setSustain(int sustain);

    @Override
    public void close() {

    }
}
