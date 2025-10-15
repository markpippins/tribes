package com.angrysurfer.beats.panel.session;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.Scale;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel for session parameters and controls (right side of toolbar)
 */
public class SessionControlPanel extends JPanel {
    private final Map<String, JComponent> fields = new HashMap<>();

    private Session currentSession;

    public SessionControlPanel() {
        super(new BorderLayout());
        setup();
    }

    private void setup() {
        // Create top and bottom panels
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);

        // Add command bus listener for scale navigation
        setupCommandBusListener();
    }

    /**
     * Sets up command bus listeners for scale navigation
     */
    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;

                // Skip processing commands from ourselves to avoid feedback loops
                if (action.getSender() == SessionControlPanel.this) return;

                // Get the scale combo box
                JComboBox<String> scaleCombo = (JComboBox<String>) fields.get("Scale");
                if (scaleCombo == null) return;

                switch (action.getCommand()) {
                    case Commands.PREV_SCALE_SELECTED:
                        if (scaleCombo.getSelectedIndex() > 0) {
                            // Move to previous scale
                            int newIndex = scaleCombo.getSelectedIndex() - 1;
                            scaleCombo.setSelectedIndex(newIndex);

                            // Check if we reached the first scale
                            if (newIndex == 0) {
                                CommandBus.getInstance().publish(Commands.FIRST_SCALE_SELECTED, this,
                                        scaleCombo.getItemAt(newIndex));
                            }
                        }
                        break;

                    case Commands.NEXT_SCALE_SELECTED:
                        if (scaleCombo.getSelectedIndex() < scaleCombo.getItemCount() - 1) {
                            // Move to next scale
                            int newIndex = scaleCombo.getSelectedIndex() + 1;
                            scaleCombo.setSelectedIndex(newIndex);

                            // Check if we reached the last scale
                            if (newIndex == scaleCombo.getItemCount() - 1) {
                                CommandBus.getInstance().publish(Commands.LAST_SCALE_SELECTED, this,
                                        scaleCombo.getItemAt(newIndex));
                            }
                        }
                        break;
                }
            }
        }, new String[]{
                Commands.PREV_SCALE_SELECTED,
                Commands.NEXT_SCALE_SELECTED
        });
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 4, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Object[][] topFieldsArray = {
                {"PPQ", createSessionCombo("PPQ", 1, 384, 24)},
                {"BPM", createSessionCombo("BPM", 1, 960, 120)},
                {"B/Bar", createSessionCombo("B/Bar", 1, 16, 4)},
                {"Bars", createSessionCombo("Bars", 1, 128, 4)},
                {"Parts", createSessionCombo("Parts", 1, 64, 1)}
        };

        for (Object[] field : topFieldsArray) {
            panel.add(createFieldPanel((String) field[0], (JComponent) field[1]));
        }

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create fields for bottom panel
        JComboBox<String> rootNoteCombo = createRootNoteCombo();
        fields.put("Root", rootNoteCombo);

        JComboBox<String> scaleCombo = createScaleCombo();
        scaleCombo.setPreferredSize(new Dimension(180, 25)); // Make scale combo wider
        fields.put("Scale", scaleCombo);

        // Create offset combo box (values from -12 to +12)
        JComboBox<Integer> offsetCombo = new JComboBox<>();
        for (int i = -24; i <= 24; i++) {
            offsetCombo.addItem(i);
        }
        offsetCombo.setSelectedItem(0);
        offsetCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentSession != null) {
                currentSession.setNoteOffset((Integer) offsetCombo.getSelectedItem());
                CommandBus.getInstance().publish(Commands.SESSION_UPDATED, this, currentSession);
            }
        });
        fields.put("Offset", offsetCombo);

        // Create length combo box (values from 1 to 32)
        JComboBox<Integer> lengthCombo = new JComboBox<>();
        for (int i = 1; i <= 32; i++) {
            lengthCombo.addItem(i);
        }
        lengthCombo.setSelectedItem(4);
        lengthCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentSession != null) {
                // Explicitly convert Integer to Long
                int selectedValue = (Integer) lengthCombo.getSelectedItem();
                currentSession.setPartLength(selectedValue); // Will be auto-boxed to Long
                CommandBus.getInstance().publish(Commands.SESSION_UPDATED, this, currentSession);
            }
        });
        fields.put("Length", lengthCombo);

        // Layout the components with appropriate sizing
        String[] rightLabels = {"Root", "Scale", "Offset", "Length"};
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 2, 0, 2);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridy = 0;

        for (int i = 0; i < rightLabels.length; i++) {
            String label = rightLabels[i];
            JPanel fieldPanel = createFieldPanel(label, fields.get(label));

            // Special handling for Scale to span 2 columns
            if (label.equals("Scale")) {
                gbc.gridwidth = 2;
            } else {
                gbc.gridwidth = 1;
            }

            gbc.gridx = (label.equals("Offset") || label.equals("Length")) ?
                    i + 1 : i; // Adjust position for Offset and Length due to Scale's double width

            panel.add(fieldPanel, gbc);
        }

        return panel;
    }

    private JPanel createFieldPanel(String label, JComponent component) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(Color.GRAY);
        labelPanel.add(nameLabel);

        fields.put(label, component);

        fieldPanel.add(labelPanel, BorderLayout.NORTH);
        fieldPanel.add(component, BorderLayout.CENTER);

        return fieldPanel;
    }

    private JComboBox<Integer> createSessionCombo(String field, int min, int max, int current) {
        JComboBox<Integer> combo = new JComboBox<>();
        for (int i = min; i <= max; i++) {
            combo.addItem(i);
        }
        combo.setSelectedItem(current);
        combo.setMaximumSize(new Dimension(70, 25));
        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && currentSession != null) {
                int value = (Integer) combo.getSelectedItem();
                updateSessionValue(field, value);
            }
        });

        return combo;
    }

    private void updateSessionValue(String field, int value) {
        if (currentSession != null) {
            try {
                switch (field) {
                    case "PPQ" -> currentSession.setTicksPerBeat(value);
                    case "BPM" -> currentSession.setTempoInBPM((float) value);
                    case "B/Bar" -> currentSession.setBeatsPerBar(value);
                    case "Bars" -> currentSession.setBars(value);
                    case "Parts" -> currentSession.setParts(value);
                }
                CommandBus.getInstance().publish(Commands.SESSION_UPDATED, this, currentSession);
            } catch (Exception ex) {
                // Log and continue
                org.slf4j.LoggerFactory.getLogger(SessionControlPanel.class).error("Error updating session value {}: {}", field, ex.getMessage(), ex);
            }
        }
    }

    private JComboBox<String> createScaleCombo() {
        String[] scaleNames = Scale.SCALE_PATTERNS.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);

        JComboBox<String> combo = new JComboBox<>(scaleNames);
        combo.setSelectedItem("Chromatic");

        // Increase width to 195px
        combo.setMaximumSize(new Dimension(195, 25));
        combo.setPreferredSize(new Dimension(195, 25));
        combo.setMinimumSize(new Dimension(195, 25));

        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedScale = (String) combo.getSelectedItem();
                int selectedIndex = combo.getSelectedIndex();

                CommandBus.getInstance().publish(Commands.SCALE_SELECTED, this, selectedScale);

                if (selectedIndex == 0) {
                    CommandBus.getInstance().publish(Commands.FIRST_SCALE_SELECTED, this, selectedScale);
                } else if (selectedIndex == combo.getItemCount() - 1) {
                    CommandBus.getInstance().publish(Commands.LAST_SCALE_SELECTED, this, selectedScale);
                }

                if (currentSession != null) {
                    currentSession.setScale(selectedScale);
                    CommandBus.getInstance().publish(
                            Commands.GLOBAL_SCALE_SELECTION_EVENT,
                            this,
                            selectedScale
                    );
                }
            }
        });

        return combo;
    }

    private JComboBox<String> createRootNoteCombo() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        JComboBox<String> combo = new JComboBox<>(noteNames);
        combo.setSelectedItem("C");

        // Reduce width
        combo.setMaximumSize(new Dimension(45, 25));
        combo.setPreferredSize(new Dimension(45, 25));
        combo.setMinimumSize(new Dimension(45, 25));

        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        combo.setEnabled(true);

        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                CommandBus.getInstance().publish(Commands.ROOT_NOTE_SELECTED, this, combo.getSelectedItem());
        });

        return combo;
    }

    public void setSession(Session session) {
        this.currentSession = session;

        if (session == null) return;

        // Update BPM field - cast float to int for combo box
        JComboBox<Integer> bpmCombo = (JComboBox<Integer>) fields.get("BPM");
        bpmCombo.setSelectedItem(session.getTempoInBPM());

        // Update other fields
        ((JComboBox<Integer>) fields.get("PPQ")).setSelectedItem(session.getTicksPerBeat());
        ((JComboBox<Integer>) fields.get("B/Bar")).setSelectedItem(session.getBeatsPerBar());
        ((JComboBox<Integer>) fields.get("Bars")).setSelectedItem(session.getBars());
        ((JComboBox<Integer>) fields.get("Parts")).setSelectedItem(session.getParts());

        // Ensure proper conversion from Long to Integer for partLength
        Integer partLength = session.getPartLength();
        ((JComboBox<Integer>) fields.get("Length")).setSelectedItem(partLength.intValue());

        ((JComboBox<Integer>) fields.get("Offset")).setSelectedItem(session.getNoteOffset());
    }
}