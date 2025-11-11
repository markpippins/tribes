package com.angrysurfer.beats.panel.modulation;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.sequencer.TimingUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

/**
 * A Turing Machine style sequencer with an 8-bit shift register
 * and controllable randomness.
 */
public class TuringMachinePanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(TuringMachinePanel.class);

    // Shift register state
    private final boolean[] register = new boolean[8];
    private final TuringMachineLED[] leds = new TuringMachineLED[8];
    private final Random random = new Random();
    private final long lastClockTime = 0;
    private int numSteps = 8;
    private int currentStep = 0;
    // Clock settings
    private boolean useExternalClock = true;
    private Timer internalClock;
    private int internalClockInterval = 200; // ms

    // Timing options
    private int ticksPerClock = 1;
    private long lastTick = 0;

    // Controls
    private Dial randomnessDial;
    private JSpinner stepsSpinner;
    private JToggleButton loopButton;
    private JButton randomizeButton;
    private JCheckBox externalClockCheckbox;
    private JComboBox<String> clockDivisionCombo;
    private Dial clockSpeedDial;
    private JPanel registerPanel;

    // CV Output value (0-1 float)
    private float outputValue = 0f;

    /**
     * Constructor - add panelNumber parameter to identify each panel
     */
    public TuringMachinePanel() {
        setLayout(new BorderLayout());

        // Use a simpler border without the title (title will be on container)
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setupComponents();
        randomizeRegister();

        // Register with CommandBus and TimingBus
        CommandBus.getInstance().register(this, new String[]{
                Commands.TIMING_UPDATE,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP
        });

        TimingBus.getInstance().register(this);

        startInternalClock();
    }

    /**
     * Make components more compact for grid layout
     */
    private void setupComponents() {
        // More compact spacing
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // More compact register display
        registerPanel = new JPanel(new GridLayout(1, 8, 2, 0));
        registerPanel.setBorder(BorderFactory.createTitledBorder("Register"));

        // Create LEDs for each bit
        for (int i = 0; i < 8; i++) {
            leds[i] = new TuringMachineLED(Integer.toString(i + 1));
            // Make LEDs smaller for grid layout
            leds[i].setPreferredSize(new Dimension(20, 20));
            registerPanel.add(leds[i]);
        }

        // Two-column layout for more compact controls
        JPanel controlPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        // Randomness control
        JPanel randomnessPanel = new JPanel(new BorderLayout());
        randomnessPanel.setBorder(BorderFactory.createTitledBorder("Randomness"));
        randomnessDial = new Dial(0, 100, 50);
        // Smaller dial
        randomnessDial.setPreferredSize(new Dimension(40, 40));
        randomnessPanel.add(randomnessDial, BorderLayout.CENTER);

        // Steps control
        JPanel stepsPanel = new JPanel(new BorderLayout());
        stepsPanel.setBorder(BorderFactory.createTitledBorder("Steps"));
        SpinnerNumberModel stepsModel = new SpinnerNumberModel(8, 1, 8, 1);
        stepsSpinner = new JSpinner(stepsModel);
        stepsPanel.add(stepsSpinner, BorderLayout.CENTER);

        // Clock division selection
        JPanel clockDivPanel = new JPanel(new BorderLayout());
        clockDivPanel.setBorder(BorderFactory.createTitledBorder("Clock Div"));
        clockDivisionCombo = new JComboBox<>(new String[]{
                "1/16", "1/8", "1/4", "1/2", "1"
        });
        clockDivPanel.add(clockDivisionCombo, BorderLayout.CENTER);

        // Button panel - combined into one panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 2, 0));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Loop button
        loopButton = new JToggleButton("Loop");
        buttonPanel.add(loopButton);

        // Randomize button
        randomizeButton = new JButton("Random");
        buttonPanel.add(randomizeButton);

        // Clock panel
        JPanel clockPanel = new JPanel(new BorderLayout());
        clockPanel.setBorder(BorderFactory.createTitledBorder("Clock"));

        // Internal/External selection
        externalClockCheckbox = new JCheckBox("External", useExternalClock);

        // Internal clock speed dial
        clockSpeedDial = new Dial(50, 1000, 200);
        clockSpeedDial.setPreferredSize(new Dimension(40, 40));

        JPanel clockControlPanel = new JPanel(new BorderLayout());
        clockControlPanel.add(externalClockCheckbox, BorderLayout.NORTH);
        clockControlPanel.add(clockSpeedDial, BorderLayout.CENTER);
        clockPanel.add(clockControlPanel);

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        JProgressBar outputBar = new JProgressBar(0, 100);
        outputBar.setStringPainted(true);

        // Add to the grid layout
        controlPanel.add(randomnessPanel);
        controlPanel.add(stepsPanel);
        controlPanel.add(clockDivPanel);
        controlPanel.add(buttonPanel);
        controlPanel.add(clockPanel);
        controlPanel.add(outputPanel);

        // Add to content panel
        contentPanel.add(registerPanel, BorderLayout.NORTH);
        contentPanel.add(controlPanel, BorderLayout.CENTER);

        // Add content panel to main panel
        add(contentPanel, BorderLayout.CENTER);

        // Add event handlers (same as before)
        setupEventHandlers();
    }

    /**
     * Setup event handlers for all components
     */
    private void setupEventHandlers() {
        // Moved from the setupComponents method to keep it clean
        // Add all the existing action listeners/change listeners here

        // Randomness dial
        randomnessDial.addChangeListener(e -> {
            if (!randomnessDial.isDragging()) {
                logger.debug("Randomness set to: {}%", randomnessDial.getValue());
            }
        });

        // Steps spinner
        stepsSpinner.addChangeListener(e -> {
            numSteps = (int) stepsSpinner.getValue();
            updateLEDLabels();
            logger.debug("Steps set to: {}", numSteps);
        });

        // Clock division
        clockDivisionCombo.addActionListener(e -> {
            int index = clockDivisionCombo.getSelectedIndex();
            switch (index) {
                case 0:
                    ticksPerClock = 24;
                    break;
                case 1:
                    ticksPerClock = 48;
                    break;
                case 2:
                    ticksPerClock = 96;
                    break;
                case 3:
                    ticksPerClock = 192;
                    break;
                case 4:
                    ticksPerClock = 384;
                    break;
                default:
                    ticksPerClock = 96;
            }
        });

        // Loop button
        loopButton.addActionListener(e -> {
            boolean looping = loopButton.isSelected();
            loopButton.setText(looping ? "Looping" : "Loop");
        });

        // Randomize button
        randomizeButton.addActionListener(e -> {
            randomizeRegister();
        });

        // External clock checkbox
        externalClockCheckbox.addActionListener(e -> {
            useExternalClock = externalClockCheckbox.isSelected();
            clockSpeedDial.setEnabled(!useExternalClock);

            if (!useExternalClock) {
                startInternalClock();
            } else {
                stopInternalClock();
            }
        });

        // Clock speed dial
        clockSpeedDial.addChangeListener(e -> {
            if (!clockSpeedDial.isDragging()) {
                internalClockInterval = clockSpeedDial.getValue();

                if (internalClock != null && internalClock.isRunning()) {
                    stopInternalClock();
                    startInternalClock();
                }
            }
        });
    }

    /**
     * Updates the LED labels to show which steps are active
     */
    private void updateLEDLabels() {
        for (int i = 0; i < 8; i++) {
            // Show step number for active steps, empty for inactive
            leds[i].setVisible(i < numSteps);
        }
        registerPanel.revalidate();
        registerPanel.repaint();
    }

    /**
     * Randomize all bits in the register
     */
    private void randomizeRegister() {
        for (int i = 0; i < 8; i++) {
            register[i] = random.nextBoolean();
            updateLED(i);
        }
        // Update output value based on current register state
        updateOutputValue();
    }

    /**
     * Update LED display for a specific bit with color based on value
     */
    private void updateLED(int position) {
        if (position >= 0 && position < 8) {
            leds[position].setBitValue(register[position]);
            leds[position].setActivePosition(position == currentStep);
        }
    }

    /**
     * Start the internal clock timer
     */
    private void startInternalClock() {
        if (internalClock != null) {
            internalClock.stop();
        }

        internalClock = new Timer(internalClockInterval, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!useExternalClock) {
                    processClock();
                }
            }
        });
        internalClock.start();
    }

    /**
     * Stop the internal clock timer
     */
    private void stopInternalClock() {
        if (internalClock != null) {
            internalClock.stop();
        }
    }

    /**
     * Process a clock pulse - shift register and update
     */
    private void processClock() {
        // Get the bit that will be shifted out
        boolean shiftedOutBit = register[0];

        // Shift all bits left by one position
        for (int i = 0; i < 7; i++) {
            register[i] = register[i + 1];
        }

        // Determine the new bit to shift in based on randomness
        boolean newBit;

        if (loopButton.isSelected()) {
            // In loop mode, always use the shifted out bit
            newBit = shiftedOutBit;
        } else {
            // Otherwise, use randomness to determine if we keep the bit or generate a new one
            int randomChance = randomnessDial.getValue();
            if (random.nextInt(100) < randomChance) {
                // Keep the shifted out bit
                newBit = shiftedOutBit;
            } else {
                // Generate a new random bit
                newBit = random.nextBoolean();
            }
        }

        // Set the new bit at the end
        register[7] = newBit;

        // Update all LEDs
        for (int i = 0; i < 8; i++) {
            updateLED(i);
        }

        // Update current step (cycles through active steps)
        currentStep = (currentStep + 1) % numSteps;

        // Highlight the current step
        for (int i = 0; i < 8; i++) {
            updateLED(i);
        }

        // Update output value based on current step
        updateOutputValue();
    }

    /**
     * Calculate output value based on current register state
     */
    private void updateOutputValue() {
        // Convert the current register state to a value between 0-1
        int sum = 0;
        int maxValue = (1 << numSteps) - 1; // 2^numSteps - 1

        for (int i = 0; i < numSteps; i++) {
            if (register[i]) {
                sum += (1 << i);
            }
        }

        outputValue = (float) sum / maxValue;

        // Publish the output value on the command bus so other modules can use it
//        CommandBus.getInstance().publish(
//                Commands.MODULATION_VALUE_CHANGED,
//                this,
//                new Object[]{"TuringMachine", outputValue}
//        );
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE:
                if (useExternalClock && action.getData() instanceof TimingUpdate update) {
                    long currentTick = update.tickCount();

                    // Process clock when we've advanced by ticksPerClock ticks
                    if (lastTick == 0 || currentTick >= lastTick + ticksPerClock) {
                        lastTick = currentTick;
                        processClock();
                    }
                }
                break;

            case Commands.TRANSPORT_START:
                lastTick = 0;
                break;

            case Commands.TRANSPORT_STOP:
                // Reset on stop if needed
                break;
        }
    }
}
