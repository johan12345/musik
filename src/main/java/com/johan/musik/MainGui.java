package com.johan.musik;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.johan.musik.tuning.*;
import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartDescriptor;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainGui {
    private static final String[] TONE_NAMES = new String[]{"C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "H"};
    private static final String[] TUNING_NAMES = new String[]{"Pythagoreische Stimmung",
            "Reine Stimmung", "Mitteltönige Stimmung", "Wohltemperierte Stimmung (Werckmeister I)",
            "Gleichtemperierte Stimmung"};
    private static final double[] OVERTONES_NONE = new double[]{1};
    private static final double[] OVERTONES_EQUAL = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
    private static final double[] OVERTONES_LINEAR = new double[]{1, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};
    private static final double[] OVERTONES_PIANO = new double[]{1, 0.66, 0.19, 0.12, 0.05, 0.04, 0.02, 0.03};
    private static final double[][] OVERTONE_AMPLITUDES = new double[][]{OVERTONES_NONE,
            OVERTONES_EQUAL, OVERTONES_LINEAR, OVERTONES_PIANO};
    private static final String[] OVERTONE_AMPLITUDES_NAMES = new String[]{"Keine",
            "Gleichmäßig", "Linear", "Klavier"};
    private JList<MidiDevice.Info> lstMidiDevices;
    private JPanel mainLayout;
    private JSpinner spnChannel;
    private JList<String> lstTuning;
    private JSpinner spnOvertoneExpansion;
    private JSpinner spnBaseTone;
    private JPanel waveformPanel;
    private ChartPanel fourierChart;
    private JList<String> lstOvertones;
    private XYSeries fourierPlot;
    private SimpleXYChartSupport waveformChart;
    private MidiDevice.Info[] deviceInfos;
    private Synthesizer receiver;
    private Tuning tuning;
    private MidiDevice device;


    public MainGui() {
        fourierChart = new ChartPanel(null);
        $$$setupUI$$$();
        deviceInfos = MidiSystem.getMidiDeviceInfo();
        lstMidiDevices.setListData(deviceInfos);
        lstMidiDevices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstMidiDevices.setSelectedIndex(1);
        lstMidiDevices.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                initialize();
            }
        });
        spnChannel.setModel(new SpinnerNumberModel(16, 0, 16, 1));
        spnChannel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                initialize();
            }
        });
        lstTuning.setListData(TUNING_NAMES);
        lstTuning.setSelectedIndex(0);
        lstTuning.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshTuning();
            }
        });
        spnOvertoneExpansion.setModel(new SpinnerNumberModel(1., 0., 5., 0.01));
        spnOvertoneExpansion.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                refreshTuning();
                receiver.setOvertoneExpansion(doubleValue(spnOvertoneExpansion));
            }
        });
        spnBaseTone.setModel(new SpinnerListModel(TONE_NAMES));
        spnBaseTone.setValue(TONE_NAMES[0]);
        spnBaseTone.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                refreshTuning();
            }
        });
        lstOvertones.setListData(OVERTONE_AMPLITUDES_NAMES);
        lstOvertones.setSelectedIndex(0);
        lstOvertones.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                receiver.setOvertoneAmplitudes(OVERTONE_AMPLITUDES[lstOvertones.getSelectedIndex()]);
            }
        });
        initialize();
        refreshTuning();

        fourierPlot = new XYSeries("Amplitude", false, false);
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(fourierPlot);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        NumberAxis yAxis = new NumberAxis("Amplitude");
        LogAxis xAxis = new LogAxis("f / Hz");
        yAxis.setRange(0, 600);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        fourierChart.setChart(new JFreeChart(plot));

        SimpleXYChartDescriptor descriptor =
                SimpleXYChartDescriptor.decimal(0, 1000, 1000, 1d, true, 44100);
        descriptor.addLineFillItems("Amplitude");
        descriptor.setChartTitle("<html><font size='+1'><b>Wellenform</b></font></html>");
        descriptor.setXAxisDescription("<html>Zeit / s</html>");
        descriptor.setYAxisDescription("<html>Amplitude</html>");
        waveformChart = ChartFactory.createSimpleXYChart(descriptor);
        waveformPanel.add(waveformChart.getChart(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 300), null, 0, false));
        receiver.addAudioThreadListener(new Synthesizer.AudioThreadListener() {
            @Override
            public void onNewSamples(double time, short[] data) {
                int i = data.length;
                for (short item : data) {
                    waveformChart.addValues((long) (Synthesizer.SAMPLE_RATE * time - i), new
                            long[]{1000L * Math.abs(item)});
                    i--;
                }
            }
        });

        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            private static final double MIN = 10;
            private static final double MAX = 4000;
            private static final double STEP = 1.01;

            @Override
            public void run() {
                double[] bins = new double[(int) (Math.log(MAX - MIN) / Math.log(STEP)) + 1];
                for (Map.Entry<Double, Double> entry : receiver.getAllTones().entrySet()) {
                    double freq = entry.getKey();
                    if (freq < MAX && freq > MIN) bins[(int) (Math.log(freq - MIN) / Math.log
                            (STEP))] += entry.getValue();
                }
                for (int i = 0; i < bins.length; i++) {
                    fourierPlot.setNotify(false);
                    fourierPlot.addOrUpdate(Math.exp(i * Math.log(STEP)) + MIN, bins[i]);
                    fourierPlot.setNotify(true);
                    fourierPlot.fireSeriesChanged();
                }
            }
        };
        timer.schedule(task, 1000, 50);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Musik");
        frame.setContentPane(new MainGui().mainLayout);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void refreshTuning() {
        switch (lstTuning.getSelectedIndex()) {
            case 0:
                tuning = new PythagoreanTuning(Arrays.binarySearch(TONE_NAMES, spnBaseTone
                        .getValue().toString()) + Tuning.C);
                spnOvertoneExpansion.setEnabled(false);
                spnBaseTone.setEnabled(true);
                break;
            case 1:
                tuning = new JustTuning(Arrays.binarySearch(TONE_NAMES, spnBaseTone
                        .getValue().toString()) + Tuning.C);
                spnOvertoneExpansion.setEnabled(false);
                spnBaseTone.setEnabled(true);
                break;
            case 2:
                tuning = new MeantoneTuning(Arrays.binarySearch(TONE_NAMES, spnBaseTone
                        .getValue().toString()) + Tuning.C);
                spnOvertoneExpansion.setEnabled(false);
                spnBaseTone.setEnabled(true);
                break;
            case 3:
                tuning = new WerckmeisterTuning(Arrays.binarySearch(TONE_NAMES, spnBaseTone
                        .getValue().toString()) + Tuning.C);
                spnOvertoneExpansion.setEnabled(false);
                spnBaseTone.setEnabled(true);
                break;
            case 4:
                tuning = new EqualTemperament(doubleValue(spnOvertoneExpansion));
                spnOvertoneExpansion.setEnabled(true);
                spnBaseTone.setEnabled(false);
                break;
        }
        receiver.setTuning(tuning);
    }

    private void initialize() {
        initialize(lstMidiDevices.getSelectedValue(), intValue(spnChannel), doubleValue
                (spnOvertoneExpansion), tuning, OVERTONE_AMPLITUDES[lstOvertones.getSelectedIndex
                ()]);
    }

    private void initialize(MidiDevice.Info info, int channel, double overtoneExpansion, Tuning
            tuning, double[] overtoneAmplitudes) {
        boolean file = false;
        if (receiver != null) receiver.close();
        if (device != null) device.close();
        try {
            if (file) {
                Sequencer sequencer = MidiSystem.getSequencer(false);
                Sequence seq = MidiSystem.getSequence(new File("wolf.mid"));
                sequencer.setSequence(seq);
                sequencer.open();
                device = sequencer;
            } else {
                device = MidiSystem.getMidiDevice(info);
                if (!(device.isOpen())) device.open();
            }
            receiver = new Synthesizer(channel, overtoneExpansion, tuning, overtoneAmplitudes);
            device.getTransmitter().setReceiver(receiver);
            if (device instanceof Sequencer) ((Sequencer) device).start();
        } catch (MidiUnavailableException | LineUnavailableException | InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }

    private int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private double doubleValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainLayout = new JPanel();
        mainLayout.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainLayout.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder("Midi-Gerät"));
        spnChannel = new JSpinner();
        panel1.add(spnChannel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Kanal");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lstMidiDevices = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        lstMidiDevices.setModel(defaultListModel1);
        panel1.add(lstMidiDevices, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainLayout.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder("Stimmung"));
        lstTuning = new JList();
        panel2.add(lstTuning, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        spnOvertoneExpansion = new JSpinner();
        panel2.add(spnOvertoneExpansion, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Spreizung");
        panel2.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spnBaseTone = new JSpinner();
        panel2.add(spnBaseTone, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Basiston");
        panel2.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainLayout.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder("Fourier-Analyse"));
        panel3.add(fourierChart, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        waveformPanel = new JPanel();
        waveformPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainLayout.add(waveformPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        waveformPanel.setBorder(BorderFactory.createTitledBorder("Wellenform"));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainLayout.add(panel4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder("Obertöne"));
        lstOvertones = new JList();
        panel4.add(lstOvertones, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainLayout;
    }
}