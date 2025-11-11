package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.panel.modulation.oscillator.LFOPanel;
import com.angrysurfer.beats.panel.modulation.oscillator.WaveformType;
import com.angrysurfer.beats.widget.DoubleDial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A panel that implements complex LFO modulation where one LFO can modulate
 * the parameters of another LFO.
 */
public class ComplexLFOPanel extends JPanel implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ComplexLFOPanel.class);
    
    // The two LFO panels (carrier and modulator)
    private LFOPanel carrierLFO;
    private LFOPanel modulatorLFO;
    
    // Modulation amount controls
    private Map<String, DoubleDial> modulationAmountDials = new HashMap<>();
    
    // Modulation enable switches
    private Map<String, JToggleButton> modulationEnableSwitches = new HashMap<>();
    
    // Output visualization
    private ComplexWaveformPanel complexWaveformDisplay;
    
    // Update thread
    private ScheduledExecutorService executor;
    private boolean running = true;
    
    // Current output value
    private double currentOutputValue = 0.0;
    
    // Value change listener
    private Consumer<Double> valueChangeListener;
    
    // Parameter names for modulation routing
    private static final String[] PARAMETERS = {
        "Frequency", "Amplitude", "Offset", "Phase", "PulseWidth"
    };
    
    /**
     * Creates a new Complex LFO Panel
     */
    public ComplexLFOPanel() {
        super(new BorderLayout(5, 5));
        // setBorder(BorderFactory.createCompoundBorder(
        //         BorderFactory.createTitledBorder("Complex LFO"),
        //         BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        initializeUI();
        startUpdateThread();
        
        logger.info("Complex LFO Panel initialized");
    }
    
    private void initializeUI() {
        // Main panel uses BorderLayout
        setLayout(new BorderLayout(5, 5));
        
        // MIDDLE ROW - LFOs and MODULATION MATRIX
        JPanel controlsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create the modulator LFO (left side)
        modulatorLFO = new LFOPanel();
        modulatorLFO.setBorder(BorderFactory.createTitledBorder("Modulator LFO"));
        
        // Create the carrier LFO (middle)
        carrierLFO = new LFOPanel();
        carrierLFO.setBorder(BorderFactory.createTitledBorder("Carrier LFO"));
        
        // Create right panel that will contain preset selector and modulation matrix
        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        
        // Create preset panel at the top of right panel
        JPanel presetPanel = createPresetPanel();
        presetPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Presets"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        rightPanel.add(presetPanel, BorderLayout.NORTH);
        
        // Create modulation matrix below preset panel
        JPanel modulationMatrixPanel = createModulationMatrixPanel();
        rightPanel.add(modulationMatrixPanel, BorderLayout.CENTER);
        
        // Add all panels to the horizontal controls panel
        controlsPanel.add(modulatorLFO);
        controlsPanel.add(carrierLFO);
        controlsPanel.add(rightPanel);
        
        // Add controls panel to main layout
        add(controlsPanel, BorderLayout.CENTER);
        
        // BOTTOM ROW - WAVEFORM DISPLAYS HORIZONTALLY
        JPanel waveformsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        waveformsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        
        // Create waveform displays
        // Modulator waveform
        ModulatorWaveformPanel modulatorWaveformPanel = new ModulatorWaveformPanel();
        JPanel modWavePanel = new JPanel(new BorderLayout());
        modWavePanel.setBorder(BorderFactory.createTitledBorder("Modulator Output"));
        modWavePanel.add(modulatorWaveformPanel, BorderLayout.CENTER);
        
        // Carrier waveform
        CarrierWaveformPanel carrierWaveformPanel = new CarrierWaveformPanel();
        JPanel carrWavePanel = new JPanel(new BorderLayout());
        carrWavePanel.setBorder(BorderFactory.createTitledBorder("Carrier Output"));
        carrWavePanel.add(carrierWaveformPanel, BorderLayout.CENTER);
        
        // Combined waveform
        complexWaveformDisplay = new ComplexWaveformPanel();
        JPanel combinedWavePanel = new JPanel(new BorderLayout());
        combinedWavePanel.setBorder(BorderFactory.createTitledBorder("Combined Output"));
        combinedWavePanel.add(complexWaveformDisplay, BorderLayout.CENTER);
        
        // Add all waveform panels to the row
        waveformsPanel.add(modWavePanel);
        waveformsPanel.add(carrWavePanel);
        waveformsPanel.add(combinedWavePanel);
        
        // Add waveforms panel to main layout
        add(waveformsPanel, BorderLayout.SOUTH);
        
        // Set value change listeners for both LFOs
        modulatorLFO.setValueChangeListener(value -> {
            modulatorWaveformPanel.setValue(value);
            onModulatorValueChanged(value);
        });
        
        carrierLFO.setValueChangeListener(value -> {
            carrierWaveformPanel.setValue(value);
            onCarrierValueChanged(value);
        });
    }
    
    private JPanel createPresetPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        panel.add(new JLabel("Preset:"));
        
        String[] presets = {
            "Default", "Vibrato", "Tremolo", "Random Wobble", "Frequency Sweep", 
            "Pulse Width Modulation", "Chaos Engine"
        };
        
        JComboBox<String> presetCombo = new JComboBox<>(presets);
        presetCombo.addActionListener(e -> {
            applyPreset(presetCombo.getSelectedIndex());
        });
        panel.add(presetCombo);
        
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter preset name:");
            if (name != null && !name.trim().isEmpty()) {
                logger.info("Saving preset: {}", name);
            }
        });
        panel.add(saveButton);
        
        return panel;
    }
    
    private JPanel createModulationMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Modulation Matrix"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Create header panel
        JPanel headerPanel = new JPanel(new GridLayout(1, 3));
        headerPanel.add(new JLabel("Parameter", SwingConstants.CENTER));
        headerPanel.add(new JLabel("Enable", SwingConstants.CENTER));
        headerPanel.add(new JLabel("Amount", SwingConstants.CENTER));
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Create matrix panel with two rows of parameters
        JPanel matrixPanel = new JPanel(new GridLayout(3, 2, 5, 10)); // 3 rows, 2 columns (2 params per row)
        
        // Parameters arranged in two columns
        for (int i = 0; i < PARAMETERS.length; i++) {
            String param = PARAMETERS[i];
            
            // Create a panel for each parameter row
            JPanel paramPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            
            // Parameter name
            JLabel nameLabel = new JLabel(param);
            nameLabel.setPreferredSize(new Dimension(75, 20));
            paramPanel.add(nameLabel);
            
            // Enable switch
            JToggleButton enableSwitch = new JToggleButton();
            enableSwitch.setPreferredSize(new Dimension(20, 20));
            enableSwitch.addActionListener(e -> updateModulation());
            paramPanel.add(enableSwitch);
            modulationEnableSwitches.put(param, enableSwitch);
            
            // Amount dial
            DoubleDial amountDial = new DoubleDial();
            amountDial.setMinimum(0.0);
            amountDial.setMaximum(1.0);
            amountDial.setValue(0.0);
            amountDial.setStepSize(0.01);
            amountDial.setToolTipText("Modulation amount for " + param);
            amountDial.addChangeListener(e -> updateModulation());
            
            // Create a panel for the dial with a value label
            JPanel dialPanel = new JPanel(new BorderLayout());
            dialPanel.add(amountDial, BorderLayout.CENTER);
            
            JLabel valueLabel = new JLabel("0.00");
            valueLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dialPanel.add(valueLabel, BorderLayout.SOUTH);
            
            // Update label when dial changes
            amountDial.addChangeListener(e -> {
                valueLabel.setText(String.format("%.2f", amountDial.getValue()));
            });
            
            paramPanel.add(dialPanel);
            modulationAmountDials.put(param, amountDial);
            
            // Add to the matrix panel
            matrixPanel.add(paramPanel);
        }
        
        panel.add(matrixPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private void applyPreset(int presetIndex) {
        // Apply different preset configurations
        switch (presetIndex) {
            case 0: // Default
                resetToDefault();
                break;
            case 1: // Vibrato
                setupVibrato();
                break;
            case 2: // Tremolo
                setupTremolo();
                break;
            case 3: // Random Wobble
                setupRandomWobble();
                break;
            case 4: // Frequency Sweep
                setupFrequencySweep();
                break;
            case 5: // Pulse Width Modulation
                setupPulseWidthModulation();
                break;
            case 6: // Chaos Engine
                setupChaosEngine();
                break;
        }
    }
    
    private void resetToDefault() {
        // Reset modulation amounts
        for (DoubleDial dial : modulationAmountDials.values()) {
            dial.setValue(0.0);
        }
        
        // Reset enable switches
        for (JToggleButton toggle : modulationEnableSwitches.values()) {
            toggle.setSelected(false);
        }
        
        // Reset carrier LFO
        carrierLFO.setFrequency(1.0);
        carrierLFO.setAmplitude(1.0);
        carrierLFO.setOffset(0.0);
        carrierLFO.setPhase(0.0);
        carrierLFO.setPulseWidth(0.5);
        
        // Reset modulator LFO
        modulatorLFO.setFrequency(0.2);
        modulatorLFO.setAmplitude(1.0);
        modulatorLFO.setOffset(0.0);
        modulatorLFO.setPhase(0.0);
        modulatorLFO.setPulseWidth(0.5);
    }
    
    private void setupVibrato() {
        resetToDefault();
        
        // Set up modulator
        modulatorLFO.setFrequency(6.0);
        modulatorLFO.setAmplitude(1.0);
        
        // Enable frequency modulation
        modulationEnableSwitches.get("Frequency").setSelected(true);
        modulationAmountDials.get("Frequency").setValue(0.3);
    }
    
    private void setupTremolo() {
        resetToDefault();
        
        // Set up modulator
        modulatorLFO.setFrequency(4.0);
        modulatorLFO.setAmplitude(1.0);
        
        // Enable amplitude modulation
        modulationEnableSwitches.get("Amplitude").setSelected(true);
        modulationAmountDials.get("Amplitude").setValue(0.7);
    }
    
    private void setupRandomWobble() {
        resetToDefault();
        
        // Set up modulator
        modulatorLFO.setFrequency(0.8);
        modulatorLFO.setAmplitude(1.0);
        // Set to random waveform
        try {
            modulatorLFO.setCurrentWaveform(WaveformType.RANDOM);
        } catch (Exception e) {
            logger.error("Could not set waveform: {}", e.getMessage());
        }
        
        // Enable multiple modulations
        modulationEnableSwitches.get("Frequency").setSelected(true);
        modulationAmountDials.get("Frequency").setValue(0.3);
        
        modulationEnableSwitches.get("Phase").setSelected(true);
        modulationAmountDials.get("Phase").setValue(0.5);
    }
    
    private void setupFrequencySweep() {
        resetToDefault();
        
        // Set up modulator
        modulatorLFO.setFrequency(0.1);
        modulatorLFO.setAmplitude(1.0);
        try {
            modulatorLFO.setCurrentWaveform(WaveformType.SAWTOOTH);
        } catch (Exception e) {
            logger.error("Could not set waveform: {}", e.getMessage());
        }
        
        // Enable frequency modulation with high depth
        modulationEnableSwitches.get("Frequency").setSelected(true);
        modulationAmountDials.get("Frequency").setValue(0.9);
    }
    
    private void setupPulseWidthModulation() {
        resetToDefault();
        
        // Set carrier to pulse
        try {
            carrierLFO.setCurrentWaveform(WaveformType.PULSE);
        } catch (Exception e) {
            logger.error("Could not set waveform: {}", e.getMessage());
        }
        
        // Set up modulator
        modulatorLFO.setFrequency(0.3);
        modulatorLFO.setAmplitude(1.0);
        try {
            modulatorLFO.setCurrentWaveform(WaveformType.TRIANGLE);
        } catch (Exception e) {
            logger.error("Could not set waveform: {}", e.getMessage());
        }
        
        // Enable pulse width modulation
        modulationEnableSwitches.get("PulseWidth").setSelected(true);
        modulationAmountDials.get("PulseWidth").setValue(0.7);
    }
    
    private void setupChaosEngine() {
        resetToDefault();
        
        // Set up carrier
        carrierLFO.setFrequency(2.0);
        carrierLFO.setAmplitude(0.8);
        carrierLFO.setOffset(0.1);
        
        // Set up modulator with fast frequency
        modulatorLFO.setFrequency(7.0);
        modulatorLFO.setAmplitude(1.0);
        try {
            modulatorLFO.setCurrentWaveform(WaveformType.RANDOM);
        } catch (Exception e) {
            logger.error("Could not set waveform: {}", e.getMessage());
        }
        
        // Enable all modulations with varying amounts
        modulationEnableSwitches.get("Frequency").setSelected(true);
        modulationAmountDials.get("Frequency").setValue(0.6);
        
        modulationEnableSwitches.get("Amplitude").setSelected(true);
        modulationAmountDials.get("Amplitude").setValue(0.4);
        
        modulationEnableSwitches.get("Offset").setSelected(true);
        modulationAmountDials.get("Offset").setValue(0.3);
        
        modulationEnableSwitches.get("Phase").setSelected(true);
        modulationAmountDials.get("Phase").setValue(0.5);
        
        modulationEnableSwitches.get("PulseWidth").setSelected(true);
        modulationAmountDials.get("PulseWidth").setValue(0.2);
    }
    
    private void startUpdateThread() {
        // Create a scheduled executor to update the modulation
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::updateModulation, 0, 16, TimeUnit.MILLISECONDS); // ~60Hz
    }
    
    private void updateModulation() {
        if (!running) return;
        
        // Get current modulator value
        double modulatorValue = modulatorLFO.getCurrentValue();
        
        // Apply modulation to carrier parameters if enabled
        if (modulationEnableSwitches.get("Frequency").isSelected()) {
            double amount = modulationAmountDials.get("Frequency").getValue();
            double baseFreq = carrierLFO.getFrequency();
            // Apply modulation: baseValue + (modulatorValue * amount * baseValue)
            // This scales the modulation relative to the base value
            double newFreq = baseFreq + (modulatorValue * amount * baseFreq);
            // Ensure frequency stays positive
            newFreq = Math.max(0.001, newFreq);
            carrierLFO.setFrequency(newFreq);
        }
        
        if (modulationEnableSwitches.get("Amplitude").isSelected()) {
            double amount = modulationAmountDials.get("Amplitude").getValue();
            double baseAmp = carrierLFO.getAmplitude();
            double newAmp = baseAmp + (modulatorValue * amount * baseAmp);
            // Ensure amplitude stays within 0-1 range
            newAmp = Math.max(0.0, Math.min(1.0, newAmp));
            carrierLFO.setAmplitude(newAmp);
        }
        
        if (modulationEnableSwitches.get("Offset").isSelected()) {
            double amount = modulationAmountDials.get("Offset").getValue();
            double baseOffset = carrierLFO.getOffset();
            double newOffset = baseOffset + (modulatorValue * amount);
            // Ensure offset stays within -1 to 1 range
            newOffset = Math.max(-1.0, Math.min(1.0, newOffset));
            carrierLFO.setOffset(newOffset);
        }
        
        if (modulationEnableSwitches.get("Phase").isSelected()) {
            double amount = modulationAmountDials.get("Phase").getValue();
            double basePhase = carrierLFO.getPhase();
            double phaseShift = modulatorValue * amount;
            // Phase wraps around 0-1
            double newPhase = (basePhase + phaseShift) % 1.0;
            if (newPhase < 0) newPhase += 1.0;
            carrierLFO.setPhase(newPhase);
        }
        
        if (modulationEnableSwitches.get("PulseWidth").isSelected()) {
            double amount = modulationAmountDials.get("PulseWidth").getValue();
            double basePW = carrierLFO.getPulseWidth();
            double newPW = basePW + (modulatorValue * amount * 0.5);
            // Ensure pulse width stays within 0.01-0.99 range
            newPW = Math.max(0.01, Math.min(0.99, newPW));
            carrierLFO.setPulseWidth(newPW);
        }
        
        // Update the complex waveform display
        SwingUtilities.invokeLater(() -> {
            complexWaveformDisplay.setModulatorValue(modulatorValue);
            complexWaveformDisplay.setCarrierValue(carrierLFO.getCurrentValue());
            complexWaveformDisplay.repaint();
        });
    }
    
    private void onModulatorValueChanged(double value) {
        // This is called when the modulator LFO value changes
        // Any additional handling can be done here
    }
    
    private void onCarrierValueChanged(double value) {
        // This is called when the carrier LFO value changes
        currentOutputValue = value;
        
        // Notify listeners if attached
        if (valueChangeListener != null) {
            valueChangeListener.accept(value);
        }
        
        // Optionally publish to command bus
        //CommandBus.getInstance().publish(
        //        Commands.LFO_VALUE_CHANGED,
        //        this,
        //        value);
    }
    
    /**
     * Set a listener to be notified when the complex LFO value changes
     */
    public void setValueChangeListener(Consumer<Double> listener) {
        this.valueChangeListener = listener;
    }
    
    /**
     * Get the current output value of the complex LFO
     */
    public double getCurrentValue() {
        return currentOutputValue;
    }
    
    @Override
    public void close() {
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Close both LFO panels
        try {
            if (carrierLFO != null) carrierLFO.close();
            if (modulatorLFO != null) modulatorLFO.close();
        } catch (Exception e) {
            logger.error("Error closing LFO panels: {}", e.getMessage());
        }
    }
    
    /**
     * Panel for visualizing the complex waveform output
     */
    private class ComplexWaveformPanel extends JPanel {
        private static final int HISTORY_SIZE = 500;
        private final java.util.Deque<Double> carrierHistory = new java.util.ArrayDeque<>(HISTORY_SIZE);
        private final java.util.Deque<Double> modulatorHistory = new java.util.ArrayDeque<>(HISTORY_SIZE);
        
        private double currentCarrierValue = 0.0;
        private double currentModulatorValue = 0.0;
        
        public ComplexWaveformPanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(400, 160));
            
            // Initialize with zeros
            for (int i = 0; i < HISTORY_SIZE; i++) {
                carrierHistory.addLast(0.0);
                modulatorHistory.addLast(0.0);
            }
        }
        
        public void setCarrierValue(double value) {
            currentCarrierValue = value;
            carrierHistory.addLast(value);
            if (carrierHistory.size() > HISTORY_SIZE) {
                carrierHistory.removeFirst();
            }
        }
        
        public void setModulatorValue(double value) {
            currentModulatorValue = value;
            modulatorHistory.addLast(value);
            if (modulatorHistory.size() > HISTORY_SIZE) {
                modulatorHistory.removeFirst();
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            int width = getWidth();
            int height = getHeight();
            int midY = height / 2;
            
            // Anti-aliasing for smoother lines
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw center line
            g2d.setColor(new Color(40, 40, 40));
            g2d.drawLine(0, midY, width, midY);
            
            // Draw grid lines
            g2d.setColor(new Color(30, 30, 30));
            g2d.drawLine(0, midY - height / 4, width, midY - height / 4);
            g2d.drawLine(0, midY + height / 4, width, midY + height / 4);
            g2d.drawLine(width / 4, 0, width / 4, height);
            g2d.drawLine(width / 2, 0, width / 2, height);
            g2d.drawLine(3 * width / 4, 0, 3 * width / 4, height);
            
            // Draw modulator waveform (dimmer)
            g2d.setColor(new Color(100, 100, 255, 128)); // Blue, semi-transparent
            g2d.setStroke(new BasicStroke(1f));
            drawWaveform(g2d, modulatorHistory, width, height, midY);
            
            // Draw carrier waveform (brighter)
            g2d.setColor(new Color(255, 100, 100)); // Red
            g2d.setStroke(new BasicStroke(2f));
            drawWaveform(g2d, carrierHistory, width, height, midY);
            
            // Draw current value indicators
            int modY = midY - (int)(currentModulatorValue * height / 2);
            int carrY = midY - (int)(currentCarrierValue * height / 2);
            
            g2d.setColor(new Color(150, 150, 255));
            g2d.fillOval(width - 10, modY - 3, 6, 6);
            
            g2d.setColor(new Color(255, 150, 150));
            g2d.fillOval(width - 10, carrY - 3, 6, 6);
            
            g2d.dispose();
        }
        
        private void drawWaveform(Graphics2D g2d, java.util.Deque<Double> history, int width, int height, int midY) {
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            
            // Convert history to array for easier indexing
            Double[] values = history.toArray(new Double[0]);
            
            for (int i = 0; i < values.length; i++) {
                // Calculate x position (newest values on the right)
                double x = ((double) i / values.length) * width;
                
                // Calculate y position (invert since Y grows downward)
                double y = midY - (values[i] * height / 2);
                
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            
            g2d.draw(path);
        }
    }
    
    /**
     * Panel for visualizing the modulator waveform output
     */
    private class ModulatorWaveformPanel extends JPanel {
        private static final int HISTORY_SIZE = 500;
        private final java.util.Deque<Double> history = new java.util.ArrayDeque<>(HISTORY_SIZE);
        private double currentValue = 0.0;
        
        public ModulatorWaveformPanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(300, 130));
            
            // Initialize with zeros
            for (int i = 0; i < HISTORY_SIZE; i++) {
                history.addLast(0.0);
            }
        }
        
        public void setValue(double value) {
            currentValue = value;
            history.addLast(value);
            if (history.size() > HISTORY_SIZE) {
                history.removeFirst();
            }
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            int width = getWidth();
            int height = getHeight();
            int midY = height / 2;
            
            // Anti-aliasing for smoother lines
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw center line
            g2d.setColor(new Color(40, 40, 40));
            g2d.drawLine(0, midY, width, midY);
            
            // Draw grid lines
            g2d.setColor(new Color(30, 30, 30));
            g2d.drawLine(0, midY - height / 4, width, midY - height / 4);
            g2d.drawLine(0, midY + height / 4, width, midY + height / 4);
            g2d.drawLine(width / 4, 0, width / 4, height);
            g2d.drawLine(width / 2, 0, width / 2, height);
            g2d.drawLine(3 * width / 4, 0, 3 * width / 4, height);
            
            // Draw waveform
            g2d.setColor(new Color(100, 100, 255)); // Blue
            g2d.setStroke(new BasicStroke(1.5f));
            
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            
            // Convert history to array for easier indexing
            Double[] values = history.toArray(new Double[0]);
            
            for (int i = 0; i < values.length; i++) {
                // Calculate x position (newest values on the right)
                double x = ((double) i / values.length) * width;
                
                // Calculate y position (invert since Y grows downward)
                double y = midY - (values[i] * height / 2);
                
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            
            g2d.draw(path);
            
            // Draw current value indicator
            int valueY = midY - (int)(currentValue * height / 2);
            g2d.setColor(new Color(150, 150, 255));
            g2d.fillOval(width - 10, valueY - 3, 6, 6);
            
            g2d.dispose();
        }
    }
    
    /**
     * Panel for visualizing the carrier waveform output
     */
    private class CarrierWaveformPanel extends JPanel {
        private static final int HISTORY_SIZE = 500;
        private final java.util.Deque<Double> history = new java.util.ArrayDeque<>(HISTORY_SIZE);
        private double currentValue = 0.0;
        
        public CarrierWaveformPanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(300, 130));
            
            // Initialize with zeros
            for (int i = 0; i < HISTORY_SIZE; i++) {
                history.addLast(0.0);
            }
        }
        
        public void setValue(double value) {
            currentValue = value;
            history.addLast(value);
            if (history.size() > HISTORY_SIZE) {
                history.removeFirst();
            }
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            int width = getWidth();
            int height = getHeight();
            int midY = height / 2;
            
            // Anti-aliasing for smoother lines
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw center line
            g2d.setColor(new Color(40, 40, 40));
            g2d.drawLine(0, midY, width, midY);
            
            // Draw grid lines
            g2d.setColor(new Color(30, 30, 30));
            g2d.drawLine(0, midY - height / 4, width, midY - height / 4);
            g2d.drawLine(0, midY + height / 4, width, midY + height / 4);
            g2d.drawLine(width / 4, 0, width / 4, height);
            g2d.drawLine(width / 2, 0, width / 2, height);
            g2d.drawLine(3 * width / 4, 0, 3 * width / 4, height);
            
            // Draw waveform
            g2d.setColor(new Color(255, 100, 100)); // Red
            g2d.setStroke(new BasicStroke(1.5f));
            
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            
            // Convert history to array for easier indexing
            Double[] values = history.toArray(new Double[0]);
            
            for (int i = 0; i < values.length; i++) {
                // Calculate x position (newest values on the right)
                double x = ((double) i / values.length) * width;
                
                // Calculate y position (invert since Y grows downward)
                double y = midY - (values[i] * height / 2);
                
                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            
            g2d.draw(path);
            
            // Draw current value indicator
            int valueY = midY - (int)(currentValue * height / 2);
            g2d.setColor(new Color(255, 150, 150));
            g2d.fillOval(width - 10, valueY - 3, 6, 6);
            
            g2d.dispose();
        }
    }
}
