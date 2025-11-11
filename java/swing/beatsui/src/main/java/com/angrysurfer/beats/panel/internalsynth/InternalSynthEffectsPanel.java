package com.angrysurfer.beats.panel.internalsynth;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;

import lombok.Getter;
import lombok.Setter;

/**
 * Panel for controlling effects parameters of a synthesizer
 */
@Getter
@Setter

public class InternalSynthEffectsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthEffectsPanel.class);
    
    // Effects control constants
    public static final int CC_EFFECT_TYPE = 91;
    public static final int CC_PARAM1 = 92;
    public static final int CC_PARAM2 = 93;
    public static final int CC_MIX = 94;
    
    private final Synthesizer synthesizer;
    private int midiChannel;
    
    // UI components
    private JSlider effectTypeSlider;
    private Dial param1Dial;
    private Dial param2Dial;
    private Dial mixDial;
    
    // Parameter group panels for dynamic labeling
    private JPanel param1Group;
    private JPanel param2Group;
    
    /**
     * Create a new Effects control panel
     * 
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel The MIDI channel to send control changes to
     */
    public InternalSynthEffectsPanel(Synthesizer synthesizer, int midiChannel) {
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
                "Effects",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));

        // Use FlowLayout for controls with consistent spacing
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));

        // Create effect type selector as a HORIZONTAL slider
        effectTypeSlider = createHorizontalEffectTypeSlider();
        
        // Create parameter dials instead of sliders
        param1Dial = createParameterDial("Size/Time", 20);
        param2Dial = createParameterDial("Decay/Fdbk", 30);
        mixDial = createParameterDial("Mix", 20);

        // Create dial groups with labels
        JPanel typeGroup = createSliderGroup("Type", effectTypeSlider);
        param1Group = createDialGroup("Size/Time", param1Dial);
        param2Group = createDialGroup("Decay/Fdbk", param2Dial);
        JPanel mixGroup = createDialGroup("Mix", mixDial);

        // Add components to panel
        add(typeGroup);
        add(param1Group);
        add(param2Group);
        add(mixGroup);

        // Add event listeners
        setupEventHandlers();
    }

    /**
     * Create a horizontal effect type slider with labels
     */
    private JSlider createHorizontalEffectTypeSlider() {
        JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, 3, 0);
        slider.setToolTipText("Effect Type");
        
        // Set up tick marks and labels
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        
        // Create tick labels
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("Reverb"));
        labelTable.put(1, new JLabel("Delay"));
        labelTable.put(2, new JLabel("Chorus"));
        labelTable.put(3, new JLabel("Drive"));
        slider.setLabelTable(labelTable);
        
        // Add FlatLaf styling
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);
        
        // Set size for horizontal slider
        slider.setPreferredSize(new Dimension(240, 50));
        
        return slider;
    }

    /**
     * Create a parameter dial with label
     */
    private Dial createParameterDial(String label, int initialValue) {
        Dial dial = UIHelper.createLabeledDial("", label, initialValue);
        dial.setPreferredSize(new Dimension(60, 60));
        return dial;
    }

    /**
     * Create a panel with a dial and label
     */
    private JPanel createDialGroup(String title, Dial dial) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBorder(BorderFactory.createTitledBorder(title));
        
        // Center the dial
        // JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        dial.setPreferredSize(new Dimension(60,60));
        // dialPanel.add(dial);
        
        group.add(dial);
        
        return group;
    }

    /**
     * Create a panel with a slider and title
     */
    private JPanel createSliderGroup(String title, JSlider slider) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBorder(BorderFactory.createTitledBorder(title));
        
        // Center the slider
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sliderPanel.add(slider);
        
        group.add(sliderPanel);
        
        return group;
    }

    /**
     * Add event listeners to all controls
     */
    private void setupEventHandlers() {
        effectTypeSlider.addChangeListener(e -> {
            if (!effectTypeSlider.getValueIsAdjusting()) {
                int effectType = effectTypeSlider.getValue();
                setControlChange(CC_EFFECT_TYPE, effectType * 32); // Scale to 0-127 range
                updateParameterLabels(effectType);
            }
        });

        param1Dial.addChangeListener(e -> {
            setControlChange(CC_PARAM1, param1Dial.getValue());
        });

        param2Dial.addChangeListener(e -> {
            setControlChange(CC_PARAM2, param2Dial.getValue());
        });

        mixDial.addChangeListener(e -> {
            setControlChange(CC_MIX, mixDial.getValue());
        });
    }

    /**
     * Update parameter labels based on the selected effect type
     * 
     * @param effectType The effect type (0-3)
     */
    private void updateParameterLabels(int effectType) {
        switch (effectType) {
            case 0: // Reverb
                param1Group.setBorder(BorderFactory.createTitledBorder("Size"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Decay"));
                break;
            case 1: // Delay
                param1Group.setBorder(BorderFactory.createTitledBorder("Time"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Feedback"));
                break;
            case 2: // Chorus
                param1Group.setBorder(BorderFactory.createTitledBorder("Depth"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Rate"));
                break;
            case 3: // Drive
                param1Group.setBorder(BorderFactory.createTitledBorder("Amount"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Tone"));
                break;
        }
    }

    /**
     * Reset all controls to their default values
     */
    public void resetToDefaults() {
        effectTypeSlider.setValue(0);     // Reverb
        param1Dial.setValue(20);          // Small room size
        param2Dial.setValue(30);          // Medium decay
        mixDial.setValue(20);             // Light mix
        
        // Update parameter labels for the default effect type
        updateParameterLabels(0);
        
        // Send these values to the synth
        updateSynthState();
    }

    /**
     * Send the current state of all controls to the synthesizer
     */
    public void updateSynthState() {
        setControlChange(CC_EFFECT_TYPE, effectTypeSlider.getValue() * 32);
        setControlChange(CC_PARAM1, param1Dial.getValue());
        setControlChange(CC_PARAM2, param2Dial.getValue());
        setControlChange(CC_MIX, mixDial.getValue());
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
                logger.error("Error setting CC {} on channel {}", ccNumber, midiChannel + 1, e);
            }
        }
    }
    
    // Getters and setters for individual control values
    
    public int getEffectType() {
        return effectTypeSlider.getValue();
    }
    
    public void setEffectType(int value) {
        effectTypeSlider.setValue(value);
        updateParameterLabels(value);
    }
    
    public int getParam1() {
        return param1Dial.getValue();
    }
    
    public void setParam1(int value) {
        param1Dial.setValue(value);
    }
    
    public int getParam2() {
        return param2Dial.getValue();
    }
    
    public void setParam2(int value) {
        param2Dial.setValue(value);
    }
    
    public int getMix() {
        return mixDial.getValue();
    }
    
    public void setMix(int value) {
        mixDial.setValue(value);
    }
}
