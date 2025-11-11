package com.angrysurfer.beats.panel.player;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.PlaybackService;
import com.angrysurfer.core.service.PlaybackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * Panel for creating a new instrument and assigning it to a player
 */
public class PlayerInstrumentPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerInstrumentPanel.class);
    private final Player player;
    private final InstrumentWrapper newInstrument;
    private JTextField nameField;
    private JComboBox<String> deviceCombo;
    private JSpinner channelSpinner;
    private JSpinner lowestNoteSpinner;
    private JSpinner highestNoteSpinner;
    private JCheckBox internalCheckbox;
    private JButton okButton;
    private JButton cancelButton;
    private JTextArea ownershipInfoArea;

    /**
     * Create a new panel for creating and assigning an instrument to a player
     */
    public PlayerInstrumentPanel(Player player) {
        super(new BorderLayout());
        this.player = player;

        // Create a new instrument (defaults will be set in initComponents)
        this.newInstrument = new InstrumentWrapper();

        initComponents();
        layoutComponents();
    }

    /**
     * Initialize the UI components
     */
    private void initComponents() {
        // Name field
        nameField = new JTextField(20);

        // Device selection
        deviceCombo = new JComboBox<>();
        for (String device : MidiService.getInstance().getOutputDeviceNames()) {
            deviceCombo.addItem(device);
        }

        // Channel spinner (0-15)
        channelSpinner = new JSpinner(new SpinnerNumberModel(
                player != null ? player.getChannel() : 0, 0, 15, 1));

        // Note range spinners
        lowestNoteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 127, 1));
        highestNoteSpinner = new JSpinner(new SpinnerNumberModel(127, 0, 127, 1));

        // Internal synth checkbox
        internalCheckbox = new JCheckBox("Internal Synth");

        // Set default values from player if available
        if (player != null) {
            // Suggested name based on player
            nameField.setText(player.getName() + " Instrument");

            // Set channel from player
            channelSpinner.setValue(player.getChannel());

            // Select internal synth by default for simplicity
            internalCheckbox.setSelected(true);

            // Update UI based on internal selection
            updateUIForInternalSelection();
        }

        // Buttons
        okButton = new JButton("Create & Assign");
        cancelButton = new JButton("Cancel");

        // Action listeners
        internalCheckbox.addActionListener(this::handleInternalChanged);
        okButton.addActionListener(this::handleOkButton);
        cancelButton.addActionListener(e -> handleCancelButton());
    }

    /**
     * Layout the components with GridBagLayout
     */
    private void layoutComponents() {
        // Main content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Name field
        contentPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(nameField, gbc);

        // Device selection
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("Device:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(deviceCombo, gbc);

        // Channel spinner
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(new JLabel("Channel:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(channelSpinner, gbc);

        // Note range
        gbc.gridx = 0;
        gbc.gridy++;
        contentPanel.add(new JLabel("Lowest Note:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(lowestNoteSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPanel.add(new JLabel("Highest Note:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(highestNoteSpinner, gbc);

        // Internal synth checkbox
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(internalCheckbox, gbc);

        // Add ownership info
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create ownership/assignment info area
        ownershipInfoArea = new JTextArea(2, 20);
        ownershipInfoArea.setEditable(false);
        ownershipInfoArea.setLineWrap(true);
        ownershipInfoArea.setWrapStyleWord(true);
        ownershipInfoArea.setBackground(new Color(230, 255, 230)); // Light green background
        ownershipInfoArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Ownership Information"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Set text based on player
        if (player != null) {
            ownershipInfoArea.setText(
                    "This instrument will be created for and assigned to:\n" +
                            player.getName() + " (Channel: " + (player.getChannel() + 1) + ")"
            );
        } else {
            ownershipInfoArea.setText("New instrument will be created without an owner.");
        }

        JScrollPane scrollPane = new JScrollPane(ownershipInfoArea);
        contentPanel.add(scrollPane, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Add panels to main layout
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Handle changes to the internal synth checkbox
     */
    private void handleInternalChanged(ActionEvent e) {
        updateUIForInternalSelection();
    }

    /**
     * Update UI based on internal synth selection
     */
    private void updateUIForInternalSelection() {
        boolean isInternal = internalCheckbox.isSelected();

        // For internal synth, disable device selection
        deviceCombo.setEnabled(!isInternal);

        // For internal synth, note range is fixed
        lowestNoteSpinner.setEnabled(!isInternal);
        highestNoteSpinner.setEnabled(!isInternal);
    }

    /**
     * Handle OK button click - create and assign the instrument
     */
    private void handleOkButton(ActionEvent e) {
        // Validate input
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a name for the instrument",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Create the instrument
            newInstrument.setName(nameField.getText().trim());

            // Set properties based on UI
            if (internalCheckbox.isSelected()) {
                newInstrument.setInternal(true);
                newInstrument.setDeviceName("Java Sound Synthesizer");
            } else {
                newInstrument.setInternal(false);
                newInstrument.setDeviceName((String) deviceCombo.getSelectedItem());
            }

            // Set channel, note range
            newInstrument.setChannel((Integer) channelSpinner.getValue());
            newInstrument.setLowestNote((Integer) lowestNoteSpinner.getValue());
            newInstrument.setHighestNote((Integer) highestNoteSpinner.getValue());

            // Initialize empty collections
            if (newInstrument.getControlCodes() == null) {
                newInstrument.setControlCodes(new ArrayList<>());
            }

            // Set default values
            newInstrument.setAvailable(true);
            newInstrument.setInitialized(true);

            // Save the instrument
            RedisService.getInstance().saveInstrument(newInstrument);

            // Register with InstrumentManager
            PlaybackService.getInstance().updateInstrument(newInstrument);

            // If we have a player, assign the instrument to it
            if (player != null) {
                // Update player's instrument
                player.setInstrument(newInstrument);
                player.setInstrumentId(newInstrument.getId());

                // Save player changes
                PlaybackService.getInstance().savePlayer(player);

                // Apply instrument settings
                PlaybackService.getInstance().applyPlayerInstrument(player);

                // Notify about player update
                CommandBus.getInstance().publish(Commands.PLAYER_UPDATE_EVENT, this, new PlayerUpdateEvent(this, player));

                // Notify about instrument assignment
                CommandBus.getInstance().publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                        new Object[]{player.getId(), newInstrument.getId()});

                // Log success
                CommandBus.getInstance().publish(
                        Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate("PlayerInstrumentPanel", "Success",
                                "Created and assigned instrument: " + newInstrument.getName())
                );
            }

            // Close dialog
            SwingUtilities.getWindowAncestor(this).dispose();

        } catch (Exception ex) {
            logger.error("Error creating instrument: {}", ex.getMessage(), ex);

            JOptionPane.showMessageDialog(this,
                    "Error creating instrument: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("PlayerInstrumentPanel", "Error",
                            "Failed to create instrument: " + ex.getMessage())
            );
        }
    }

    /**
     * Handle cancel button - close dialog
     */
    private void handleCancelButton() {
        SwingUtilities.getWindowAncestor(this).dispose();
    }
}
