package com.angrysurfer.beats.panel.modulation.oscillator;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DoubleDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.TimingUpdate;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A panel that implements a Low Frequency Oscillator with various waveform
 * types
 * and visualization.
 */
@Getter
@Setter
public class LFOPanel extends JPanel implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LFOPanel.class.getName());

    // LFO parameters
    private double frequency = 1.0; // Hz
    private double amplitude = 1.0; // 0-1 range
    private double offset = 0.0; // -1 to 1 range (center position)
    private double phase = 0.0; // 0-1 range (0-360 degrees)
    private double pulseWidth = 0.5; // 0-1 range (duty cycle for pulse waves)

    // UI components
    private WaveformPanel waveformPanel;
    private LiveWaveformPanel liveWaveformPanel;
    private JComboBox<String> waveformCombo;
    private DoubleDial freqDial;
    private DoubleDial ampDial;
    private DoubleDial offsetDial;
    private DoubleDial phaseDial;
    private DoubleDial pulseWidthDial;
    private JLabel valueLabel;
    private JToggleButton runButton;
    private JToggleButton syncButton;
    private JSlider bipolarSlider; // Shows current value as a slider

    // Update thread
    private ScheduledExecutorService executor;
    private boolean running = false;
    private boolean syncMode = false;
    private long startTimeMs = 0;

    // Current waveform type and value
    private WaveformType currentWaveform = WaveformType.SINE;
    private double currentValue = 0.0;

    // Value change listener
    private Consumer<Double> valueChangeListener;
    private JComponent incrementButton;

    // Timing listener
    private IBusListener timingListener;
    private double currentBeat;

    // New fields for timing division and tempo
    private JSlider divisionSlider;
    private JLabel divisionLabel;
    private double timingDivision = 1.0; // Default 1 cycle per bar
    private double lastTickValue = 0;
    private double tempo = 120.0; // Default tempo, will be updated from TimingUpdate

    /**
     * Creates a new LFO Panel
     */
    public LFOPanel() {
        super(new BorderLayout(2, 2));
        // setBorder(new EmptyBorder(10, 10, 10, 10));
        setBorder(BorderFactory.createTitledBorder("LFO"));

        // Anonymous LFO implementation
        final LFO lfo = new LFO() {
            @Override
            public double getValue(double timeInSeconds) {
                // Calculate the oscillator value based on time and current parameters
                switch (currentWaveform) {
                    case SINE:
                        return offset
                                + amplitude * Math.sin(2 * Math.PI * frequency * timeInSeconds + phase * 2 * Math.PI);

                    case TRIANGLE:
                        double triPhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (triPhase < 0.5 ? 4 * triPhase - 1 : 3 - 4 * triPhase);

                    case SAWTOOTH:
                        double sawPhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (2 * sawPhase - 1);

                    case SQUARE:
                        double sqrPhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (sqrPhase < 0.5 ? 1 : -1);

                    case PULSE:
                        double pulsePhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (pulsePhase < pulseWidth ? 1 : -1);

                    case RANDOM:
                        // Step random - changes at frequency rate
                        double randomPhase = Math.floor(frequency * timeInSeconds + phase);
                        // Use the phase as seed for deterministic randomness
                        return offset + amplitude * (2 * ((Math.sin(randomPhase * 12345.67) + 1) % 1.0) - 1);

                    default:
                        return 0.0;
                }
            }
        };

        initializeUI();
        startLFO(lfo);
    }

    private void initializeUI() {
        // Create main panels
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        JPanel waveformPanelContainer = new JPanel(new BorderLayout(5, 5));
        JPanel topControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JPanel dialPanel = new JPanel(new GridLayout(1, 5, 5, 5));

        // Create waveform visualization panel - MAKE IT SMALLER
        waveformPanel = new WaveformPanel();
        waveformPanel.setPreferredSize(new Dimension(600, 80)); // Reduced height to ~1/4
        waveformPanelContainer.setBorder(BorderFactory.createTitledBorder("Waveform Shape"));
        waveformPanelContainer.add(waveformPanel, BorderLayout.CENTER);

        // Create live waveform panel to replace the value display
        liveWaveformPanel = new LiveWaveformPanel();
        liveWaveformPanel.setMinimumSize(new Dimension(600, 200));
        liveWaveformPanel.setPreferredSize(new Dimension(600, 200));
        JPanel liveWaveformContainer = new JPanel(new BorderLayout(5, 5));
        liveWaveformContainer.setBorder(BorderFactory.createTitledBorder("Live Output"));
        liveWaveformContainer.add(liveWaveformPanel, BorderLayout.CENTER);

        // Create compact value display
        JPanel valueDisplayPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        valueLabel = new JLabel("0.00");
        valueLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        valueDisplayPanel.add(new JLabel("Current:"));
        valueDisplayPanel.add(valueLabel);
        liveWaveformContainer.add(valueDisplayPanel, BorderLayout.SOUTH);

        // Create waveform selector
        String[] waveforms = {"Sine", "Triangle", "Sawtooth", "Square", "Pulse", "Random"};
        waveformCombo = new JComboBox<>(waveforms);
        waveformCombo.setSelectedIndex(0);
        waveformCombo.addActionListener(e -> {
            currentWaveform = WaveformType.values()[waveformCombo.getSelectedIndex()];
            updateWaveformDisplay();
            logger.info("Waveform changed to: " + currentWaveform);
        });

        // Create run button
        runButton = new JToggleButton("Run");
        runButton.setSelected(true);
        runButton.addActionListener(e -> {
            running = runButton.isSelected();
            if (running) {
                startTimeMs = System.currentTimeMillis();
            }
            runButton.setText(running ? "Stop" : "Run");
        });

        // Create control dials with enhanced panels and tooltips - UPDATED STEP SIZE
        // VALUES
        JPanel freqPanel = createDialPanel(
                "Frequency",
                "Controls the oscillation rate in Hertz (cycles per second)",
                0.001, 20.0, frequency, 0.1, // Keep 0.01 step for frequency
                val -> {
                    frequency = val;
                    updateWaveformDisplay();
                });
        freqDial = findDialInPanel(freqPanel);

        JPanel ampPanel = createDialPanel(
                "Amplitude",
                "Controls the height/strength of the waveform (0-1)",
                0.0, 1.0, amplitude, 0.01, // ULTRA-FINE STEP for finer control
                val -> {
                    amplitude = val;
                    updateWaveformDisplay();
                });
        ampDial = findDialInPanel(ampPanel);

        JPanel offsetPanel = createDialPanel(
                "Offset",
                "Shifts the center position of the waveform up or down",
                -1.0, 1.0, offset, 0.01, // ULTRA-FINE STEP for finer control
                val -> {
                    offset = val;
                    updateWaveformDisplay();
                });
        offsetDial = findDialInPanel(offsetPanel);

        JPanel phasePanel = createDialPanel(
                "Phase",
                "Shifts the starting position within the waveform cycle (0-360°)",
                0.0, 1.0, phase, 0.01, // SMALLER STEP for finer control
                val -> {
                    phase = val;
                    updateWaveformDisplay();
                });
        phaseDial = findDialInPanel(phasePanel);

        JPanel pwPanel = createDialPanel(
                "Pulse Width",
                "Controls the duty cycle of pulse waveforms (ratio of high to low time)",
                0.01, 0.99, pulseWidth, 0.001, // ULTRA-FINE STEP for finer control
                val -> {
                    pulseWidth = val;
                    updateWaveformDisplay();
                });
        pulseWidthDial = findDialInPanel(pwPanel);

        // Add dial panels (not dials) to the dial panel
        dialPanel.add(freqPanel);
        dialPanel.add(ampPanel);
        dialPanel.add(offsetPanel);
        dialPanel.add(phasePanel);
        dialPanel.add(pwPanel);

        // Build top control panel
        topControlPanel.add(new JLabel("Waveform:"));
        topControlPanel.add(waveformCombo);
        topControlPanel.add(Box.createHorizontalStrut(20));
        topControlPanel.add(runButton);

        // Add sync button
        syncButton = new JToggleButton("Sync");
        syncButton.setToolTipText("Synchronize with timing events");
        syncButton.addActionListener(e -> {
            syncMode = syncButton.isSelected();
            if (syncMode) {
                // Disable run button when in sync mode
                runButton.setSelected(false);
                running = false;
                runButton.setText("Run");
                runButton.setEnabled(false);

                // Register for timing events
                registerForTimingEvents();
            } else {
                // Re-enable run button when not in sync mode
                runButton.setEnabled(true);
                unregisterFromTimingEvents();
            }
            logger.info("Sync mode " + (syncMode ? "enabled" : "disabled"));
        });
        topControlPanel.add(syncButton);

        // Add the division slider
        JPanel divisionPanel = new JPanel(new BorderLayout(5, 0));
        divisionPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        divisionLabel = new JLabel("1/1");
        divisionLabel.setPreferredSize(new Dimension(36, 16));
        divisionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        divisionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        // Create slider for timing division - CORRECTLY REVERSED for fractions of cycle
        // per bar
        String[] divisions = {"1/1", "1/2", "1/4", "1/8", "1/16", "1/32"};
        divisionSlider = new JSlider(JSlider.HORIZONTAL, 0, divisions.length - 1, 0); // Default 1/1
        divisionSlider.setPreferredSize(new Dimension(120, 20));
        divisionSlider.setSnapToTicks(true);
        divisionSlider.setPaintTicks(true);
        divisionSlider.setMajorTickSpacing(1);
        divisionSlider.addChangeListener(e -> {
            int index = divisionSlider.getValue();
            divisionLabel.setText(divisions[index]);

            // Calculate FRACTION of cycle per bar, not cycles per bar
            // 0 = 1 cycle per bar, 1 = 1/2 cycle per bar, etc.
            double cyclesPerBar = 1.0 / Math.pow(2, index);

            // Store the timing division as this fractional value
            timingDivision = cyclesPerBar;

            logger.debug("LFO set to " + divisions[index] + " (" + cyclesPerBar + " cycles per bar)");
        });

        divisionPanel.add(new JLabel("Division:"), BorderLayout.WEST);
        divisionPanel.add(divisionSlider, BorderLayout.CENTER);
        divisionPanel.add(divisionLabel, BorderLayout.EAST);

        // Add the division panel to top controls
        topControlPanel.add(divisionPanel);

        // Add components to main panels
        controlPanel.add(topControlPanel, BorderLayout.NORTH);
        controlPanel.add(dialPanel, BorderLayout.CENTER);
        controlPanel.add(liveWaveformContainer, BorderLayout.SOUTH); // Use the live waveform instead

        // Add components to main panel
        add(waveformPanelContainer, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Update waveform display initially
        updateWaveformDisplay();
    }

    /**
     * Helper method to find and extract the DoubleDial component from a panel
     */
    private DoubleDial findDialInPanel(JPanel panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof DoubleDial) {
                return (DoubleDial) c;
            } else if (c instanceof JPanel) {
                DoubleDial found = findDialInPanel((JPanel) c);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Creates a Dial with label and value display
     */
    private DoubleDial createDial(String name, double min, double max, double initial, double step,
                                  Consumer<Double> changeListener) {
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.setBorder(BorderFactory.createTitledBorder(name));

        DoubleDial dial = new DoubleDial();
        dial.setMinimum(min);
        dial.setMaximum(max);
        dial.setValue(initial);
        dial.setStepSize(step);

        JLabel valueLabel = new JLabel(String.format("%.2f", initial));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        dial.addChangeListener(e -> {
            double value = dial.getValue();
            valueLabel.setText(String.format("%.2f", value));
            changeListener.accept(value);
        });

        panel.add(dial, BorderLayout.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);

        return dial;
    }

    /**
     * Creates a Dial with proper label, value display, and tooltip
     * Updated to add increment/decrement buttons for step size control
     */
    private JPanel createDialPanel(String name, String tooltip, double min, double max, double initial, double step,
                                   Consumer<Double> changeListener) {
        // Create panel with border layout
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(name),
                BorderFactory.createEmptyBorder(5, 5, 5, 5))); // Increased padding

        // Create the dial
        DoubleDial dial = new DoubleDial();
        dial.setMinimum(min);
        dial.setMaximum(max);
        dial.setValue(initial);
        dial.setStepSize(step);
        dial.setToolTipText(tooltip);

        // Format pattern based on step size - show more decimals for finer steps
        int decimals = step < 0.001 ? 4 : 3;
        String formatPattern = "%." + decimals + "f";

        // Create value label with formatted current value
        JLabel valueLabel = new JLabel(String.format(formatPattern, initial));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        valueLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        // Add change listener to update value label
        dial.addChangeListener(e -> {
            double value = dial.getValue();
            valueLabel.setText(String.format(formatPattern, value));
            changeListener.accept(value);
        });

        // Create units label if appropriate
        JLabel unitsLabel = null;
        if (name.equals("Frequency")) {
            unitsLabel = new JLabel("Hz");
            unitsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            unitsLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        }

        // Create center panel to hold the dial with proper centering
        JPanel dialCenterPanel = new JPanel(new GridBagLayout()); // GridBagLayout centers components
        dialCenterPanel.add(dial); // This will center the dial in the panel

        // Create increment/decrement buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));

        // Determine appropriate step multipliers based on parameter type
        double smallerStep, largerStep;

        if (name.equals("Frequency")) {
            smallerStep = step / 10; // Even finer control for frequency
            largerStep = step * 10; // Coarser adjustment
        } else if (name.equals("Pulse Width")) {
            smallerStep = 0.0005; // Very fine for pulse width
            largerStep = 0.01; // Larger step
        } else {
            smallerStep = step / 5; // Finer control
            largerStep = step * 5; // Coarser adjustment
        }

        // Create decrement button (makes steps smaller)
        JButton decrementButton = new JButton("÷");
        decrementButton.setFont(new Font("Dialog", Font.BOLD, 10));
        decrementButton.setPreferredSize(new Dimension(20, 20));
        decrementButton.setToolTipText("Decrease step size to " + String.format("%.5f", smallerStep));
        decrementButton.addActionListener(e -> {
            double currentStep = dial.getStepSize();
            dial.setStepSize(smallerStep);
            decrementButton.setEnabled(smallerStep > min / 1000); // Disable if too small
            valueLabel.setText(String.format(formatPattern, dial.getValue()));
            // Update tooltip with new value
            decrementButton.setToolTipText("Step size decreased to " + String.format("%.5f", smallerStep));
            incrementButton.setToolTipText("Increase step size to " + String.format("%.5f", currentStep));
        });

        // Create increment button (makes steps larger)
        JButton incrementButton = new JButton("×");
        incrementButton.setFont(new Font("Dialog", Font.BOLD, 10));
        incrementButton.setPreferredSize(new Dimension(20, 20));
        incrementButton.setToolTipText("Increase step size to " + String.format("%.5f", largerStep));
        incrementButton.addActionListener(e -> {
            double currentStep = dial.getStepSize();
            dial.setStepSize(largerStep);
            incrementButton.setEnabled(largerStep < (max - min) / 2); // Disable if too large
            valueLabel.setText(String.format(formatPattern, dial.getValue()));
            // Update tooltip with new value
            incrementButton.setToolTipText("Step size increased to " + String.format("%.5f", largerStep));
            decrementButton.setToolTipText("Decrease step size to " + String.format("%.5f", currentStep));
        });

        // Add buttons to panel
        buttonPanel.add(decrementButton);
        buttonPanel.add(incrementButton);

        // Create a combined panel for value display and buttons
        JPanel southPanel = new JPanel(new BorderLayout(2, 2));

        // Layout for labels (value + units)
        JPanel labelPanel = new JPanel(new BorderLayout(2, 0));
        labelPanel.add(valueLabel, BorderLayout.CENTER);
        if (unitsLabel != null) {
            labelPanel.add(unitsLabel, BorderLayout.EAST);
        }

        southPanel.add(labelPanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add components to panel
        panel.add(dialCenterPanel, BorderLayout.CENTER); // Centered dial
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Update the waveform display based on current parameters
     */
    private void updateWaveformDisplay() {
        waveformPanel.repaint();
    }

    /**
     * Start the LFO calculation thread
     */
    private void startLFO(LFO lfo) {
        running = true;
        startTimeMs = System.currentTimeMillis();

        // Create a scheduled executor to update the LFO value
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (running) {
                double timeInSeconds = (System.currentTimeMillis() - startTimeMs) / 1000.0;
                double[] newValue = {lfo.getValue(timeInSeconds)};
                // Clamp the value between -1 and 1
                newValue[0] = Math.max(-1.0, Math.min(1.0, newValue[0]));

                // Update UI on the EDT
                SwingUtilities.invokeLater(() -> {
                    currentValue = newValue[0];
                    valueLabel.setText(String.format("%.2f", newValue[0]));

                    // Update the live waveform panel with the new value
                    liveWaveformPanel.addValue(newValue[0]);

                    // Notify listeners if attached
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(newValue[0]);
                    }

                    // Publish to command bus
                    // CommandBus.getInstance().publish(
                    // Commands.LFO_VALUE_CHANGED,
                    // this,
                    // newValue[0]);
                });
            }
        }, 0, 16, TimeUnit.MILLISECONDS); // ~60 Hz update rate
    }

    /**
     * Set a listener to be notified when the LFO value changes
     */
    public void setValueChangeListener(Consumer<Double> listener) {
        this.valueChangeListener = listener;
    }

    /**
     * Get the current LFO value
     */
    public double getCurrentValue() {
        return currentValue;
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Register this LFO as a listener for timing events
     */
    private void registerForTimingEvents() {
        // Create a timing listener that follows the same pattern as SessionDisplayPanel
        if (timingListener == null) {
            timingListener = new IBusListener() {
                @Override
                public void onAction(Command action) {
                    if (action.getCommand() == Commands.TIMING_UPDATE && syncMode) {
                        if (action.getData() instanceof TimingUpdate update) {
                            // Get the tick value from the update
                            if (update.tick() != null) {
                                // Use SwingUtilities.invokeLater to ensure UI updates happen on EDT
                                SwingUtilities.invokeLater(() -> {
                                    processTimingUpdate(update);
                                });
                            }

                            // Also capture tempo if available
                            // if (update.tempo() != null) {
                            // tempo = update.tempo();
                            // }
                        }
                    }
                }
            };
        }

        // Register with the timing bus
        TimingBus.getInstance().register(timingListener);
        logger.debug("Registered for timing events");
    }

    /**
     * Process a timing update and calculate LFO value
     */
    private void processTimingUpdate(TimingUpdate update) {
        if (!syncMode)
            return;

        // Calculate phase based on timing update
        Long tickValue = update.tick();
        if (tickValue == null)
            return;

        lastTickValue = tickValue;

        // Assuming 96 ticks per bar (24 PPQN, 4/4 time)
        double ticksPerBar = 96.0;

        // Calculate phase based on position within bar and cycles per bar
        // DON'T use modulo here - we need continuous time to avoid rectification
        double normalizedPosition = tickValue / ticksPerBar;

        // Apply the timing division as a fraction
        double phase = normalizedPosition * timingDivision;

        // Keep just the fractional part for phase (0-1)
        phase = phase - Math.floor(phase);

        // Update LFO value based on this phase
        updateLFOValueFromPhase(phase);
    }

    /**
     * Update the LFO value based on a phase (0-1 range)
     */
    private void updateLFOValueFromPhase(double phase) {
        // Calculate the oscillator value using existing formulas but with phase
        double value = 0;

        switch (currentWaveform) {
            // Make sure this sine wave formula is calculating correctly
            case SINE:
                value = offset + amplitude * Math.sin(2 * Math.PI * phase + this.phase * 2 * Math.PI);
                break;
            case TRIANGLE:
                double triPhase = (phase + this.phase) % 1.0;
                value = offset + amplitude * (triPhase < 0.5 ? 4 * triPhase - 1 : 3 - 4 * triPhase);
                break;
            case SAWTOOTH:
                double sawPhase = (phase + this.phase) % 1.0;
                value = offset + amplitude * (2 * sawPhase - 1);
                break;
            case SQUARE:
                double sqrPhase = (phase + this.phase) % 1.0;
                value = offset + amplitude * (sqrPhase < 0.5 ? 1 : -1);
                break;
            case PULSE:
                double pulsePhase = (phase + this.phase) % 1.0;
                value = offset + amplitude * (pulsePhase < pulseWidth ? 1 : -1);
                break;
            case RANDOM:
                // For random, we want a stable value that changes only at divisions
                int step = (int) (lastTickValue / (24.0 / (timingDivision / 4.0)));
                value = offset + amplitude * (2 * ((Math.sin(step * 12345.67) + 1) % 1.0) - 1);
                break;
        }

        // Clamp the value between -1 and 1
        value = Math.max(-1.0, Math.min(1.0, value));

        // Update UI elements
        currentValue = value;
        valueLabel.setText(String.format("%.2f", value));

        // Update the live waveform panel
        liveWaveformPanel.addValue(value);

        // Notify listeners if attached
        if (valueChangeListener != null) {
            valueChangeListener.accept(value);
        }
    }

    /**
     * Unregister from timing events
     */
    private void unregisterFromTimingEvents() {
        if (timingListener != null) {
            TimingBus.getInstance().unregister(timingListener);
            logger.debug("Unregistered from timing events");
        }
    }

    private JButton createStepButton(String label, Dial dial, double step) {
        JButton button = new JButton(label);
        button.setPreferredSize(new Dimension(20, 20));
        button.setMinimumSize(new Dimension(20, 20));
        button.setMaximumSize(new Dimension(20, 20));
        button.setFont(new Font("Monospaced", Font.PLAIN, 8));
        button.setToolTipText("Set step size to " + step);

        button.addActionListener(e -> {
            // dial.setStepSize(step);
            // Optionally highlight the active step button
            resetStepButtonStyles();
            button.setBackground(Color.LIGHT_GRAY);
        });

        return button;
    }

    private void resetStepButtonStyles() {
        // Reset all step buttons to default style
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                for (Component subComp : ((JPanel) comp).getComponents()) {
                    if (subComp instanceof JButton) {
                        subComp.setBackground(null);
                    }
                }
            }
        }
    }

    /**
     * Panel for visualizing the waveform
     */
    public class WaveformPanel extends JPanel {
        private static final int SAMPLES = 500;

        public WaveformPanel() {
            setBackground(Color.BLACK);
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
            // Horizontal grid lines at 25%, 50%, 75%
            g2d.drawLine(0, midY - height / 4, width, midY - height / 4);
            g2d.drawLine(0, midY + height / 4, width, midY + height / 4);

            // Vertical grid lines at 25%, 50%, 75%
            g2d.drawLine(width / 4, 0, width / 4, height);
            g2d.drawLine(width / 2, 0, width / 2, height);
            g2d.drawLine(3 * width / 4, 0, 3 * width / 4, height);

            // Draw waveform
            g2d.setColor(new Color(0, 200, 120));
            g2d.setStroke(new BasicStroke(2f));

            Path2D.Double path = new Path2D.Double();
            boolean first = true;

            // To ensure we draw exactly one cycle
            double timeScale = 1.0 / frequency;

            for (int i = 0; i < SAMPLES; i++) {
                double time = (i / (double) SAMPLES) * timeScale;
                double value = 0;

                // Use the same calculations as in the LFO anonymous class
                switch (currentWaveform) {
                    case SINE:
                        value = offset + amplitude * Math.sin(2 * Math.PI * frequency * time + phase * 2 * Math.PI);
                        break;

                    case TRIANGLE:
                        double triPhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (triPhase < 0.5 ? 4 * triPhase - 1 : 3 - 4 * triPhase);
                        break;

                    case SAWTOOTH:
                        double sawPhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (2 * sawPhase - 1);
                        break;

                    case SQUARE:
                        double sqrPhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (sqrPhase < 0.5 ? 1 : -1);
                        break;

                    case PULSE:
                        double pulsePhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (pulsePhase < pulseWidth ? 1 : -1);
                        break;

                    case RANDOM:
                        // For visualization, use a step function
                        int steps = 16; // number of steps to show
                        double randomPhase = Math.floor(steps * (frequency * time + phase)) / steps;
                        // Use phase as seed for deterministic randomness
                        value = offset + amplitude * (2 * ((Math.sin(randomPhase * 12345.67) + 1) % 1.0) - 1);
                        break;
                }

                // Clamp value to -1 to 1 range
                value = Math.max(-1.0, Math.min(1.0, value));

                // Map to Y coordinate (invert because Y grows downward)
                double x = (i / (double) SAMPLES) * width;
                double y = midY - (value * height / 2);

                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.draw(path);

            // Draw value marker (current position in the waveform)
            if (running) {
                double currentPhase = (System.currentTimeMillis() - startTimeMs) / 1000.0 * frequency;
                currentPhase = currentPhase % 1.0;

                int markerX = (int) (currentPhase * width);
                g2d.setColor(Color.WHITE);
                g2d.drawLine(markerX, 0, markerX, height);
            }

            g2d.dispose();
        }
    }
}
