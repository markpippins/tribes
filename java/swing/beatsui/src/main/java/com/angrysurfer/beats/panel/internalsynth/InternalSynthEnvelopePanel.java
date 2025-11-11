package com.angrysurfer.beats.panel.internalsynth;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Hashtable;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel for controlling ADSR envelope parameters of a synthesizer
 */
@Getter
@Setter

public class InternalSynthEnvelopePanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthEnvelopePanel.class);
    
    // Envelope control constants
    public static final int CC_ATTACK = 73;
    public static final int CC_DECAY = 75;
    public static final int CC_SUSTAIN = 79;
    public static final int CC_RELEASE = 72;
    
    private final Synthesizer synthesizer;
    private int midiChannel;
    
    // UI components
    private JSlider attackSlider;
    private JSlider decaySlider;
    private JSlider sustainSlider;
    private JSlider releaseSlider;
    
    /**
     * Create a new Envelope control panel
     * 
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel The MIDI channel to send control changes to
     */
    public InternalSynthEnvelopePanel(Synthesizer synthesizer, int midiChannel) {
        super();
        this.synthesizer = synthesizer;
        this.midiChannel = midiChannel;
        
        initializeUI();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Envelope",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));

        // Use FlowLayout for sliders in a row
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));

        // Create ADSR sliders
        attackSlider = createVerticalSlider("Attack Time", 10);
        decaySlider = createVerticalSlider("Decay Time", 50);
        sustainSlider = createVerticalSlider("Sustain Level", 80);
        releaseSlider = createVerticalSlider("Release Time", 20);

        // Create slider groups with labels
        JPanel attackGroup = createSliderGroup("Attack", attackSlider);
        JPanel decayGroup = createSliderGroup("Decay", decaySlider);
        JPanel sustainGroup = createSliderGroup("Sustain", sustainSlider);
        JPanel releaseGroup = createSliderGroup("Release", releaseSlider);

        // Add labeled sliders to envelope panel
        add(attackGroup);
        add(decayGroup);
        add(sustainGroup);
        add(releaseGroup);

        // Add event listeners
        setupEventHandlers();
    }
    
    /**
     * Add event listeners to all controls
     */
    private void setupEventHandlers() {
        attackSlider.addChangeListener(e -> {
            if (!attackSlider.getValueIsAdjusting()) {
                setControlChange(CC_ATTACK, attackSlider.getValue());
            }
        });

        decaySlider.addChangeListener(e -> {
            if (!decaySlider.getValueIsAdjusting()) {
                setControlChange(CC_DECAY, decaySlider.getValue());
            }
        });

        sustainSlider.addChangeListener(e -> {
            if (!sustainSlider.getValueIsAdjusting()) {
                setControlChange(CC_SUSTAIN, sustainSlider.getValue());
            }
        });

        releaseSlider.addChangeListener(e -> {
            if (!releaseSlider.getValueIsAdjusting()) {
                setControlChange(CC_RELEASE, releaseSlider.getValue());
            }
        });
    }
    
    /**
     * Reset all controls to their default values
     */
    public void resetToDefaults() {
        attackSlider.setValue(10);     // Fast attack
        decaySlider.setValue(50);      // Medium decay  
        sustainSlider.setValue(80);    // High sustain
        releaseSlider.setValue(20);    // Short release
        
        // Send these values to the synth
        updateSynthState();
    }
    
    /**
     * Send the current state of all controls to the synthesizer
     */
    public void updateSynthState() {
        setControlChange(CC_ATTACK, attackSlider.getValue());
        setControlChange(CC_DECAY, decaySlider.getValue());
        setControlChange(CC_SUSTAIN, sustainSlider.getValue());
        setControlChange(CC_RELEASE, releaseSlider.getValue());
    }
    
    /**
     * Set a MIDI CC value on the synth
     * 
     * @param ccNumber The CC number to set
     * @param value The value to set (0-127)
     */
    private void setControlChange(int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel midiCh = synthesizer.getChannels()[midiChannel];
                if (midiCh != null) {
                    midiCh.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                logger.error("Error setting CC {} on channel {}: {}", ccNumber, (midiChannel + 1), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Create a slider with a label underneath
     */
    private JPanel createSliderGroup(String title, JSlider slider) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));

        // Center the slider
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sliderPanel.add(slider);

        // Add label at bottom
        JLabel label = new JLabel(title);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        group.add(sliderPanel);
        group.add(label);

        return group;
    }

    /**
     * Create a vertical slider with consistent styling
     * 
     * @param tooltip Tooltip text
     * @param initialValue Initial value (0-127)
     * @return Configured JSlider
     */
    private JSlider createVerticalSlider(String tooltip, int initialValue) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 127, initialValue);
        slider.setToolTipText(tooltip);

        // Set up tick marks
        slider.setMajorTickSpacing(32);
        slider.setMinorTickSpacing(16);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(false);

        // Create tick labels - just show a few key values
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(32, new JLabel("32"));
        labelTable.put(64, new JLabel("64"));
        labelTable.put(96, new JLabel("96"));
        labelTable.put(127, new JLabel("127"));
        slider.setLabelTable(labelTable);

        // FlatLaf properties
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);

        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));

        return slider;
    }
    
    // Getters and setters for individual control values
    
    public int getAttack() {
        return attackSlider.getValue();
    }
    
    public void setAttack(int value) {
        attackSlider.setValue(value);
    }
    
    public int getDecay() {
        return decaySlider.getValue();
    }
    
    public void setDecay(int value) {
        decaySlider.setValue(value);
    }
    
    public int getSustain() {
        return sustainSlider.getValue();
    }
    
    public void setSustain(int value) {
        sustainSlider.setValue(value);
    }
    
    public int getRelease() {
        return releaseSlider.getValue();
    }
    
    public void setRelease(int value) {
        releaseSlider.setValue(value);
    }
}
