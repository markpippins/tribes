package com.angrysurfer.beats.panel.sample;

import com.angrysurfer.core.model.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SamplePropertiesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SamplePropertiesPanel.class);
    // Map to store property controls for easy access
    private final Map<String, JComponent> propertyControls = new HashMap<>();
    private final SamplePropertyChangeListener listener;
    // Sample reference
    private Sample sample;

    public SamplePropertiesPanel(SamplePropertyChangeListener listener) {
        this.listener = listener;

        // Set up the panel
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Sample Properties"));
        setPreferredSize(new Dimension(180, 400));

        // Create and add the property table
        add(createPropertyTable(), BorderLayout.CENTER);
    }

    private JScrollPane createPropertyTable() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Row counter
        int row = 0;

        // Helper method to sort properties alphabetically
        String[] propertyNames = {
                "Auto-Trim",
                "Fade In",
                "Fade In Duration (ms)",
                "Fade Out",
                "Fade Out Duration (ms)",
                "Loop Enabled",
                "Loop End",
                "Loop Start",
                "Normalize",
                "Reverse",
                "Sample End",
                "Sample Start"
        };

        // Add all properties in alphabetical order
        for (String propertyName : propertyNames) {
            // Add label in first column
            c.gridx = 0;
            c.gridy = row;
            c.weightx = 0.4;
            panel.add(new JLabel(propertyName), c);

            // Add appropriate control in second column
            c.gridx = 1;
            c.gridy = row;
            c.weightx = 0.6;

            JComponent control = createControlForProperty(propertyName);
            control.setPreferredSize(new Dimension(80, 20));
            control.setMaximumSize(new Dimension(80, 20));
            control.setMinimumSize(new Dimension(80, 20));
            panel.add(control, c);
            propertyControls.put(propertyName, control);

            row++;
        }

        // Add a glue at the bottom to push everything up
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), c);

        // Wrap in a scroll pane in case there are too many properties
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        return scrollPane;
    }

    private JComponent createControlForProperty(String propertyName) {
        switch (propertyName) {
            // Boolean properties - checkboxes
            case "Auto-Trim":
                JCheckBox autoTrimCheck = new JCheckBox();
                autoTrimCheck.addActionListener(e -> {
                    if (sample != null) {
                        sample.setAutoTrimEnabled(autoTrimCheck.isSelected());
                        notifyPropertyChanged();
                    }
                });
                return autoTrimCheck;

            case "Fade In":
                JCheckBox fadeInCheck = new JCheckBox();
                fadeInCheck.addActionListener(e -> {
                    if (sample != null) {
                        sample.setFadeInEnabled(fadeInCheck.isSelected());
                        notifyPropertyChanged();
                    }
                });
                return fadeInCheck;

            case "Fade Out":
                JCheckBox fadeOutCheck = new JCheckBox();
                fadeOutCheck.addActionListener(e -> {
                    if (sample != null) {
                        sample.setFadeOutEnabled(fadeOutCheck.isSelected());
                        notifyPropertyChanged();
                    }
                });
                return fadeOutCheck;

            case "Loop Enabled":
                JCheckBox loopEnabledCheck = new JCheckBox();
                loopEnabledCheck.addActionListener(e -> {
                    if (sample != null) {
                        sample.setLoopEnabled(loopEnabledCheck.isSelected());
                        notifyPropertyChanged();
                    }
                });
                return loopEnabledCheck;

            case "Normalize":
                JCheckBox normalizeCheck = new JCheckBox();
                normalizeCheck.addActionListener(e -> {
                    if (sample != null) {
                        sample.setNormalizeEnabled(normalizeCheck.isSelected());
                        notifyPropertyChanged();
                    }
                });
                return normalizeCheck;

            case "Reverse":
                JCheckBox reverseCheck = new JCheckBox();
                reverseCheck.addActionListener(e -> {
                    if (sample != null) {
                        sample.setReverseEnabled(reverseCheck.isSelected());
                        notifyPropertyChanged();
                    }
                });
                return reverseCheck;

            // Integer properties - spinners
            case "Fade In Duration (ms)":
                JSpinner fadeInSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 10));
                fadeInSpinner.addChangeListener(e -> {
                    if (sample != null) {
                        sample.setFadeInDuration((Integer) fadeInSpinner.getValue());
                        notifyPropertyChanged();
                    }
                });
                return fadeInSpinner;

            case "Fade Out Duration (ms)":
                JSpinner fadeOutSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 10));
                fadeOutSpinner.addChangeListener(e -> {
                    if (sample != null) {
                        sample.setFadeOutDuration((Integer) fadeOutSpinner.getValue());
                        notifyPropertyChanged();
                    }
                });
                return fadeOutSpinner;

            case "Loop Start":
                JSpinner loopStartSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100));
                loopStartSpinner.addChangeListener(e -> {
                    if (sample != null) {
                        sample.setLoopStart((Integer) loopStartSpinner.getValue());
                        notifyPropertyChanged();
                    }
                });
                return loopStartSpinner;

            case "Loop End":
                JSpinner loopEndSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100));
                loopEndSpinner.addChangeListener(e -> {
                    if (sample != null) {
                        sample.setLoopEnd((Integer) loopEndSpinner.getValue());
                        notifyPropertyChanged();
                    }
                });
                return loopEndSpinner;

            case "Sample Start":
                JSpinner sampleStartSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100));
                sampleStartSpinner.addChangeListener(e -> {
                    if (sample != null) {
                        sample.setSampleStart((Integer) sampleStartSpinner.getValue());
                        notifyPropertyChanged();
                    }
                });
                return sampleStartSpinner;

            case "Sample End":
                JSpinner sampleEndSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 100));
                sampleEndSpinner.addChangeListener(e -> {
                    if (sample != null) {
                        sample.setSampleEnd((Integer) sampleEndSpinner.getValue());
                        notifyPropertyChanged();
                    }
                });
                return sampleEndSpinner;

            default:
                return new JLabel("N/A");
        }
    }

    /**
     * Set the sample to edit
     */
    public void setSample(Sample sample) {
        this.sample = sample;
        updateControlValues();
    }

    /**
     * Update UI controls based on sample properties
     */
    private void updateControlValues() {
        if (sample == null) return;

        // Update checkbox states
        updateCheckbox("Auto-Trim", sample.isAutoTrimEnabled());
        updateCheckbox("Fade In", sample.isFadeInEnabled());
        updateCheckbox("Fade Out", sample.isFadeOutEnabled());
        updateCheckbox("Loop Enabled", sample.isLoopEnabled());
        updateCheckbox("Normalize", sample.isNormalizeEnabled());
        updateCheckbox("Reverse", sample.isReverseEnabled());

        // Update spinner values
        updateSpinner("Fade In Duration (ms)", sample.getFadeInDuration());
        updateSpinner("Fade Out Duration (ms)", sample.getFadeOutDuration());
        updateSpinner("Loop Start", sample.getLoopStart());
        updateSpinner("Loop End", sample.getLoopEnd());
        updateSpinner("Sample Start", sample.getSampleStart());
        updateSpinner("Sample End", sample.getSampleEnd());
    }

    private void updateCheckbox(String propertyName, boolean value) {
        JComponent component = propertyControls.get(propertyName);
        if (component instanceof JCheckBox) {
            ((JCheckBox) component).setSelected(value);
        }
    }

    private void updateSpinner(String propertyName, int value) {
        JComponent component = propertyControls.get(propertyName);
        if (component instanceof JSpinner) {
            ((JSpinner) component).setValue(value);
        }
    }

    private void notifyPropertyChanged() {
        if (listener != null && sample != null) {
            listener.onPropertyChanged(sample);
        }
    }

    // Listener interface for property changes
    public interface SamplePropertyChangeListener {
        void onPropertyChanged(Sample sample);
    }
}
