package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.panel.MainPanel;
import com.angrysurfer.beats.widget.DrumSelectorButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerInstrumentChangeEvent;
import com.angrysurfer.core.event.PlayerPresetChangeEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel containing drum pad selectors
 */
public class DrumSelectorPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(DrumSelectorPanel.class);

    // Create drum buttons for standard drum kit sounds
    private static final String[] drumNames = {
            "Kick", "Snare", "Closed HH", "Open HH",
            "Tom 1", "Tom 2", "Tom 3", "Crash",
            "Ride", "Rim", "Clap", "Cow",
            "Clave", "Shaker", "Perc 1", "Perc 2"
    };

    private static final int MIDI_DRUM_NOTE_OFFSET = SequencerConstants.MIDI_DRUM_NOTE_OFFSET;

    // Default MIDI notes for General MIDI drums
    private static final int[] defaultNotes = {
            MIDI_DRUM_NOTE_OFFSET, MIDI_DRUM_NOTE_OFFSET + 2, MIDI_DRUM_NOTE_OFFSET + 6, MIDI_DRUM_NOTE_OFFSET + 10,
            MIDI_DRUM_NOTE_OFFSET + 5, MIDI_DRUM_NOTE_OFFSET + 7, MIDI_DRUM_NOTE_OFFSET + 9,
            MIDI_DRUM_NOTE_OFFSET + 13,
            MIDI_DRUM_NOTE_OFFSET + 15, MIDI_DRUM_NOTE_OFFSET + 1, MIDI_DRUM_NOTE_OFFSET + 3,
            MIDI_DRUM_NOTE_OFFSET + 20,
            MIDI_DRUM_NOTE_OFFSET + 39, MIDI_DRUM_NOTE_OFFSET + 34, MIDI_DRUM_NOTE_OFFSET + 24,
            MIDI_DRUM_NOTE_OFFSET + 25
    };

    // Constants
    private static final int DRUM_PAD_COUNT = SequencerConstants.DRUM_PAD_COUNT;
    // Reference to the sequencer and parent panel
    private final DrumSequencer sequencer;
    private final DrumSequencerGridPanel parentPanel;
    // UI components
    private final List<DrumSelectorButton> drumButtons = new ArrayList<>();

    /**
     * Creates a new DrumSelectorPanel
     *
     * @param sequencer   The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSelectorPanel(DrumSequencer sequencer, DrumSequencerGridPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;

        // Use GridLayout for perfect vertical alignment with grid cells
        setLayout(new GridLayout(DRUM_PAD_COUNT, 1, 2, 2));
        setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        initializeButtons();

        // Register for events - adding SYSTEM_READY
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_UPDATE_EVENT,
                Commands.PLAYER_PRESET_CHANGED,
                Commands.PLAYER_PRESET_CHANGE_EVENT,
                Commands.PLAYER_INSTRUMENT_CHANGED,
                Commands.DRUM_PLAYER_INSTRUMENT_CHANGED,
                Commands.SOUNDBANK_CHANGED,
                Commands.SYSTEM_READY  // Add this for startup initialization
        });
    }

    /**
     * Update the onAction method to properly handle all preset change cases
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.SYSTEM_READY:
                // Slight delay to ensure all managers are fully initialized
                Timer timer = new Timer(100, evt -> {
                    logger.info("System ready - initializing drum selector buttons");
                    // Make sure all players are properly initialized with instruments

                    for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                        Player player = sequencer.getPlayer(i);
                        if (player != null) {
                            // Ensure player has instrument
                            if (player.getInstrument() == null) {
                                logger.warn("Player {} has no instrument, initializing default", SequencerConstants.MELODIC_CHANNELS[i]);
                                sequencer.ensurePlayerHasInstrument(i);
                                player = sequencer.getPlayer(i); // Get refreshed reference
                            }

                            // Make sure each drum has the right sound name from InternalSynthManager
                            if (player.getRootNote() != null) {
                                String drumName = com.angrysurfer.core.service.MidiService.getInstance()
                                        .getDrumName(player.getRootNote());
                                player.setName(drumName);
                                logger.debug("Set player {} name to: {}", i, drumName);
                            }
                        }
                    }

                    // Update all buttons with player info
                    refreshAllButtons();

                    // Force repaint of the entire panel
                    revalidate();
                    repaint();
                });
                timer.setRepeats(false);
                timer.start();
                break;

            case Commands.PLAYER_UPDATE_EVENT:
                if (action.getData() instanceof PlayerUpdateEvent event) {
                    // Check if this player belongs to our sequencer
                    if (event.getPlayer().getOwner() == sequencer) {
                        updateButtonForPlayer(event.getPlayer());
                        logger.debug("Updated drum button for player: {}", event.getPlayer().getName());
                    }
                }

                break;

            case Commands.PLAYER_PRESET_CHANGED, Commands.PLAYER_PRESET_CHANGE_EVENT:
                if (action.getData() instanceof Object[] data && data.length >= 2) {
                    Long playerId = (Long) data[0];

                    // Find player and update button if it belongs to this sequencer
                    for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                        Player player = sequencer.getPlayer(i);
                        if (player != null && playerId.equals(player.getId())) {
                            // First, refresh the player to ensure we have the latest data
                            sequencer.refreshPlayer(i);
                            player = sequencer.getPlayer(i); // Get updated reference

                            updateButtonForPlayer(player);
                            logger.debug("Updated drum button for player {} after preset change",
                                    player.getName());
                            break;
                        }
                    }
                } else if (action.getData() instanceof PlayerPresetChangeEvent event) {
                    Player eventPlayer = event.getPlayer();
                    if (eventPlayer != null) {
                        // Check if this player belongs to our sequencer
                        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                            Player player = sequencer.getPlayer(i);
                            if (player != null && player.getId().equals(eventPlayer.getId())) {
                                // Ensure our player is updated with latest name
                                sequencer.refreshPlayer(i);
                                updateButtonForDrumPad(i);
                                logger.debug("Updated drum button {} after preset change event for: {}",
                                        i, player.getName());
                                break;
                            }
                        }
                    }
                }
                break;

            case Commands.PLAYER_INSTRUMENT_CHANGED:
                if (action.getData() instanceof Object[] data && data.length >= 2) {
                    Long playerId = (Long) data[0];

                    // Find player and update button if it belongs to this sequencer
                    for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                        Player player = sequencer.getPlayer(i);
                        if (player != null && playerId.equals(player.getId())) {
                            // Refresh the player to ensure we have the latest data
                            sequencer.refreshPlayer(i);
                            player = sequencer.getPlayer(i);
                            updateButtonForPlayer(player);
                            logger.debug("Updated drum button for player {} after instrument change",
                                    player.getName());
                            break;
                        }
                    }
                }
                break;

            case Commands.DRUM_PLAYER_INSTRUMENT_CHANGED:
                if (action.getData() instanceof PlayerInstrumentChangeEvent event) {
                    updateButtonForPlayer(event.getPlayer());
                }
                if (action.getData() instanceof Object[] data && data.length >= 3) {
                    DrumSequencer targetSequencer = (DrumSequencer) data[0];
                    int drumIndex = (int) data[1];

                    // Only update if this is our sequencer
                    if (targetSequencer == sequencer && drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
                        Player player = sequencer.getPlayer(drumIndex);
                        if (player != null) {
                            updateButtonForPlayer(player);
                        }
                    }
                }
                break;

            // Add handler for soundbank changes which also need to update buttons
            case Commands.SOUNDBANK_CHANGED:
                if (action.getData() instanceof Player player) {
                    // Check if this player belongs to our sequencer
                    for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                        Player sequencerPlayer = sequencer.getPlayer(i);
                        if (sequencerPlayer != null && sequencerPlayer.getId().equals(player.getId())) {
                            sequencer.refreshPlayer(i);
                            updateButtonForDrumPad(i);
                            logger.debug("Updated drum button {} after soundbank change for: {}",
                                    i, player.getName());
                            break;
                        }
                    }
                }
                break;
        }
    }

    /**
     * Update button text and tooltip for a player
     */
    private void updateButtonForPlayer(Player player) {
        // Find which drum pad this player belongs to
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            if (sequencer.getPlayer(i) == player) {
                // We found the right player, update the corresponding button
                updateButtonForDrumPad(i);
                break;
            }
        }
    }

    /**
     * Update button text and tooltip for a specific drum pad with more detailed info
     */
    private void updateButtonForDrumPad(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= drumButtons.size()) {
            return;
        }

        // Get the button and player
        DrumSelectorButton button = drumButtons.get(drumIndex);
        Player player = sequencer.getPlayer(drumIndex);

        if (player == null) {
            return;
        }

        // Use SwingUtilities.invokeLater to ensure thread safety
        SwingUtilities.invokeLater(() -> {
            try {
                // Update button text with player name (which should reflect preset)
                String buttonText = player.getName();

                // Add instrument info if available
                if (player.getInstrument() != null) {
                    String instrumentInfo = "";

                    // If button text would be too long, use a shorter representation
                    if ((buttonText + instrumentInfo).length() > 25) {
                        buttonText = buttonText.substring(0, Math.min(buttonText.length(), 20)) +
                                instrumentInfo;
                    } else {
                        buttonText += instrumentInfo;
                    }
                }

                button.setText(buttonText);

                // Update tooltip with detailed information
                StringBuilder tooltip = new StringBuilder();
                tooltip.append("Select ").append(player.getName());

                // Add MIDI note information
                Integer noteNumber = player.getRootNote();
                if (noteNumber != null) {
                    tooltip.append(" (Note: ").append(noteNumber).append(")");
                }

                // Add instrument information
                if (player.getInstrument() != null) {
                    tooltip.append("<br>Instrument: ").append(player.getInstrument().getName());

                    if (player.getInstrument().getPreset() != null) {
                        tooltip.append("<br>Preset: ").append(player.getInstrument().getPreset());
                    }

                    if (player.getInstrument().getBankIndex() != null) {
                        tooltip.append("<br>Bank: ").append(player.getInstrument().getBankIndex());
                    }

                    if (player.getInstrument().getSoundbankName() != null) {
                        tooltip.append("<br>Soundbank: ").append(player.getInstrument().getSoundbankName());
                    }
                }

                button.setToolTipText("<html>" + tooltip + "</html>");

                logger.debug("Updated drum button {} to '{}' (Note: {})",
                        drumIndex, buttonText, noteNumber);

                // Force button to repaint
                button.invalidate();
                button.repaint();
            } catch (Exception e) {
                logger.error("Error updating button for drum {}: {}", drumIndex, e.getMessage(), e);
            }
        });
    }

    /**
     * Initialize all drum selector buttons
     */
    private void initializeButtons() {


        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            final int drumIndex = i;

            // Get the existing Player from the sequencer instead of creating a new Strike
            Player player = sequencer.getPlayer(drumIndex);

            // Configure the player if it exists
            if (player != null) {
                // Set name and root note only if they haven't been set yet
                if (player.getName() == null || player.getName().isEmpty()) {
                    player.setName(drumNames[i]);
                }

                // Only update root note if not already set to a custom value
                // Checks if it matches default sequencer initialization value
                if (player.getRootNote() == MIDI_DRUM_NOTE_OFFSET + i) {
                    player.setRootNote(defaultNotes[i]);
                }

                // Set default velocity if not already set
                if (player.getLevel() <= 0) {
                    player.setLevel(100);
                }

                logger.debug("Using existing player for drum {}: {}", i, player.getName());
            } else {
                logger.warn("Player for drum {} is null", i);
            }

            // Create the drum button with proper selection handling
            DrumSelectorButton drumButton = new DrumSelectorButton(drumIndex, sequencer);

            // Use the player's name if available, otherwise use default
            String buttonText = (player != null && player.getName() != null) ?
                    player.getName() : drumNames[i];
            drumButton.setText(buttonText);

            // Show MIDI note in tooltip
            int noteNumber = (player != null) ? player.getRootNote() : defaultNotes[i];
            drumButton.setToolTipText("Select " + buttonText + " (Note: " + noteNumber + ")");

            // Rest of the button setup remains the same
            drumButton.addActionListener(e -> {
                parentPanel.selectDrumPad(drumIndex);
            });

            // Add double-click support to navigate to params panel
            drumButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Select the drum pad first
                        parentPanel.selectDrumPad(drumIndex);

                        // Find the parent tabbed pane that contains "Parameters" tab
                        // This approach works regardless of the nesting structure
                        Container parent = parentPanel;
                        while (parent != null) {
                            if (parent instanceof JTabbedPane tabbedPane) {
                                // Check if this tabbed pane has a "Parameters" tab

                                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                                    if ("Parameters".equals(tabbedPane.getTitleAt(i))) {
                                        // Found it - switch to the Parameters tab
                                        tabbedPane.setSelectedIndex(i);
                                        logger.debug("Double-clicked drum pad {} - switched to Parameters tab", drumIndex);
                                        return;
                                    }
                                }
                            }
                            parent = parent.getParent();
                        }

                        logger.warn("Could not find Parameters tab when double-clicking drum pad {}", drumIndex);
                    }
                }
            });

            // Update key listener as well for consistency
            drumButton.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        // Select the drum pad first
                        parentPanel.selectDrumPad(drumIndex);

                        // Find the MainPanel ancestor
                        MainPanel mainPanel = findMainPanel();
                        if (mainPanel != null) {
                            // Get the currently selected component from the main tabbed pane
                            Component selectedComponent = mainPanel.getSelectedComponent();

                            // If it's a JTabbedPane and likely our drumsTabbedPane
                            if (selectedComponent instanceof JTabbedPane drumsTabbedPane) {

                                // Switch to the "Parameters" tab (index 1)
                                drumsTabbedPane.setSelectedIndex(1);

                                logger.debug("Enter pressed on drum pad {} - switched to Parameters tab", drumIndex);
                            }
                        }
                    }
                }
            });

            drumButton.setFocusable(true);
            drumButtons.add(drumButton);
            add(drumButton);
        }
    }

    /**
     * Update all buttons to reflect current player names and settings
     */
    public void refreshAllButtons() {
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            updateButtonForDrumPad(i);
        }
    }

    // Add helper method to find MainPanel ancestor
    private MainPanel findMainPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof MainPanel) {
                return (MainPanel) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }


    /**
     * Update the appearance of all drum buttons
     */
    public void updateButtonSelection(int selectedIndex) {
        for (int i = 0; i < drumButtons.size(); i++) {
            drumButtons.get(i).setSelected(i == selectedIndex);
        }
    }

    public Integer getSelectedDrumPadIndex() {
        for (int i = 0; i < drumButtons.size(); i++) {
            if (drumButtons.get(i).isSelected()) {
                return i;
            }
        }
        return null; // No drum pad selected
    }
}
