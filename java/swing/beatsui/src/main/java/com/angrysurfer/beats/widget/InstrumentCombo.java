package com.angrysurfer.beats.widget;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerInstrumentChangeEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

/**
 * Refactored InstrumentCombo with better manager integration
 */
@Getter
@Setter
public class InstrumentCombo extends JComboBox<InstrumentWrapper> implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentCombo.class);

    private Player currentPlayer;
    private boolean isInitializing = false;
    private boolean isUpdatingSelection = false; // New flag to prevent recursion

    /**
     * Create a new InstrumentCombo that leverages InstrumentManager
     */
    public InstrumentCombo() {
        super();
        configureRenderer();
        CommandBus.getInstance().register(this, new String[]{Commands.PLAYER_SELECTION_EVENT,
                Commands.INSTRUMENTS_REFRESHED, Commands.INSTRUMENT_UPDATED, Commands.PLAYER_UPDATE_EVENT,
                Commands.PLAYER_INSTRUMENT_CHANGED});

        setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH * 2, UIHelper.CONTROL_HEIGHT));
        setMinimumSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH * 2, UIHelper.CONTROL_HEIGHT));
        // Add action listener to handle selection changes
        addActionListener(e -> {
            if (isInitializing || isUpdatingSelection) return;
            handleSelectionChange();
        });
    }

    /**
     * Configure the cell renderer for instruments
     */
    private void configureRenderer() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof InstrumentWrapper instrument && currentPlayer != null) {
                    Integer playerChannel = currentPlayer.getChannel();

                    // Show instrument device type in display
                    boolean isInternal = instrument.getInternal() != null && instrument.getInternal();
                    String deviceInfo = isInternal ? "[Internal]" :
                            (instrument.getDeviceName() != null ? "[" + instrument.getDeviceName() + "]" : "");

                    // Check if this is for current player's channel
                    boolean isForChannel = (playerChannel != null) &&
                            isInstrumentForChannel(instrument, playerChannel);

                    // Add channel indicator
                    String channelInfo = "";
                    if (instrument.getChannel() != null) {
                        channelInfo = " (Ch:" + (instrument.getChannel() + 1) + ")";
                    }

                    // Format text - add star for channel-appropriate instruments
                    String displayText = (isForChannel ? "★ " : "") +
                            instrument.getName() + channelInfo + " " + deviceInfo;

                    setText(displayText);

                    // Add bold font for channel-appropriate instruments
                    if (isForChannel && !isSelected) {
                        setFont(getFont().deriveFont(Font.BOLD));
                        if (!isSelected) {
                            // setBackground(new Color(240, 255, 240)); // Light green background
                        }
                    }

                    // Set tooltip with detailed info
                    setToolTipText(instrument.getName() +
                            (instrument.getDescription() != null ?
                                    " - " + instrument.getDescription() : "") +
                            " - Channel: " + (instrument.getChannel() != null ?
                            (instrument.getChannel() + 1) : "Not set"));
                }

                return c;
            }
        });
    }

    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.PLAYER_SELECTION_EVENT:
                if (action.getData() instanceof Player player) {
                    setCurrentPlayer(player);
                }
                break;

            case Commands.PLAYER_UPDATE_EVENT:
                if (action.getData() instanceof PlayerUpdateEvent event &&
                        currentPlayer != null &&
                        event.getPlayer().getId().equals(currentPlayer.getId())) {
                    // Only update if instrument has changed
                    if (event.getPlayer().getInstrumentId() != null &&
                            (currentPlayer.getInstrumentId() == null ||
                                    !event.getPlayer().getInstrumentId().equals(currentPlayer.getInstrumentId()))) {
                        updateSelectedInstrument(event.getPlayer());
                    }
                }
                break;

            case Commands.PLAYER_INSTRUMENT_CHANGED:
                // Handle instrument change notification
                if (action.getData() instanceof Object[] data && data.length >= 2) {
                    Long playerId = (Long) data[0];
                    Long instrumentId = (Long) data[1];

                    if (currentPlayer != null &&
                            playerId != null &&
                            playerId.equals(currentPlayer.getId())) {
                        // Instead of full update, just select the right instrument
                        selectInstrumentById(instrumentId);
                    }
                }
                break;

            case Commands.INSTRUMENTS_REFRESHED:
            case Commands.INSTRUMENT_UPDATED:
                // Reload instruments if we have a current player
                if (currentPlayer != null) {
                    populateInstruments();
                }
                break;
        }
    }

    /**
     * Set the current player and update the combo accordingly
     */
    public void setCurrentPlayer(Player player) {
        if (player == null) return;

        currentPlayer = player;
        SwingUtilities.invokeLater(this::populateInstruments);
    }

    /**
     * Update selected instrument to match the player
     * Fixed to prevent recursion
     */
    public void updateSelectedInstrument(Player player) {
        if (player == null || player.getInstrument() == null) return;

        // Set flag to prevent recursion
        isUpdatingSelection = true;
        try {
            currentPlayer = player;

            // Find and select the matching instrument
            Long instrumentId = player.getInstrumentId();
            if (instrumentId == null) return;

            // Find in combo - iterate through items
            boolean found = false;
            for (int i = 0; i < getItemCount(); i++) {
                InstrumentWrapper comboItem = getItemAt(i);
                if (comboItem != null && comboItem.getId() != null &&
                        comboItem.getId().equals(instrumentId)) {
                    setSelectedIndex(i);
                    found = true;
                    break;
                }
            }

            // If not found, log it but don't attempt to reload to prevent recursion
            if (!found) {
                logger.warn("Instrument with ID {} not found in combo ({})",
                        instrumentId, player.getName());
            }

        } finally {
            isUpdatingSelection = false;
        }
    }

    /**
     * Populate the combo with instruments through InstrumentManager
     * Fixed to show all available instruments
     */
    public void populateInstruments() {
        // Check for recursion
        if (isInitializing || isUpdatingSelection) {
            logger.debug("Skipping populateInstruments due to recursion prevention");
            return;
        }

        isInitializing = true;
        try {
            removeAllItems();

            if (currentPlayer == null) {
                logger.warn("No player set for InstrumentCombo");
                return;
            }

            try {
                Integer playerChannel = currentPlayer.getChannel();
                if (playerChannel == null) {
                    logger.warn("Player has no channel set");
                    return;
                }

                List<InstrumentWrapper> allInstruments = getAllInstrumentWrappers(playerChannel);

                // Add all instruments to combo
                if (!allInstruments.isEmpty()) {
                    for (InstrumentWrapper instrument : allInstruments) {
                        // Only add available instruments
                        //if (instrument != null &&
                        //      (instrument.getAvailable() == null || instrument.getAvailable())) {
                        addItem(instrument);
                        // }
                    }

                    logger.info("Added {} instruments to combo", getItemCount());
                } else {
                    logger.warn("No instruments found");
                }

                // Select the current instrument if set
                selectPlayerInstrument();

            } catch (Exception e) {
                logger.error("Error populating instruments: {}", e.getMessage(), e);
            }
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Get all instrument wrappers, sorted in a user-friendly way
     * - Channel-appropriate instruments first
     * - Alphabetical order with proper numerical sorting
     */
    private List<InstrumentWrapper> getAllInstrumentWrappers(Integer playerChannel) {
        List<InstrumentWrapper> allInstruments =
                RedisService.getInstance().getInstrumentHelper().findAllInstruments();

        // Sort instruments with enhanced alphanumeric sorting
        allInstruments.sort((a, b) -> {
            // Primary sort: channel-appropriate instruments first
            boolean aForChannel = isInstrumentForChannel(a, playerChannel);
            boolean bForChannel = isInstrumentForChannel(b, playerChannel);

            if (aForChannel && !bForChannel) return -1;
            if (!aForChannel && bForChannel) return 1;

            // Secondary sort: by name with numerical awareness
            return compareNaturalOrder(a.getName(), b.getName());
        });

        return allInstruments;
    }

    /**
     * Compare strings with awareness of embedded numbers
     * Ensures "Default Drum 2" comes before "Default Drum 10"
     */
    private int compareNaturalOrder(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        // Implementation of natural sort for alphanumeric strings
        int aIndex = 0, bIndex = 0;

        while (aIndex < a.length() && bIndex < b.length()) {
            // Skip leading spaces
            while (aIndex < a.length() && Character.isWhitespace(a.charAt(aIndex))) aIndex++;
            while (bIndex < b.length() && Character.isWhitespace(b.charAt(bIndex))) bIndex++;

            // If we've reached the end of either string
            if (aIndex >= a.length() || bIndex >= b.length()) {
                break;
            }

            // If both characters are digits, extract and compare the numbers
            if (Character.isDigit(a.charAt(aIndex)) && Character.isDigit(b.charAt(bIndex))) {
                // Extract the numbers
                int aNum = 0;
                while (aIndex < a.length() && Character.isDigit(a.charAt(aIndex))) {
                    aNum = aNum * 10 + (a.charAt(aIndex) - '0');
                    aIndex++;
                }

                int bNum = 0;
                while (bIndex < b.length() && Character.isDigit(b.charAt(bIndex))) {
                    bNum = bNum * 10 + (b.charAt(bIndex) - '0');
                    bIndex++;
                }

                // Compare numbers
                if (aNum != bNum) {
                    return Integer.compare(aNum, bNum);
                }
            }
            // If not both digits, compare characters
            else {
                char aChar = Character.toLowerCase(a.charAt(aIndex));
                char bChar = Character.toLowerCase(b.charAt(bIndex));

                if (aChar != bChar) {
                    return aChar - bChar;
                }

                aIndex++;
                bIndex++;
            }
        }

        // If we've exhausted one string but not the other
        return Integer.compare(a.length() - aIndex, b.length() - bIndex);
    }

    /**
     * Helper method to select the player's instrument in the combo
     */
    private void selectPlayerInstrument() {
        if (currentPlayer == null || currentPlayer.getInstrumentId() == null) return;

        Long instrumentId = currentPlayer.getInstrumentId();

        // Find matching instrument in the combo
        for (int i = 0; i < getItemCount(); i++) {
            InstrumentWrapper item = getItemAt(i);
            if (item != null && item.getId() != null &&
                    item.getId().equals(instrumentId)) {
                setSelectedIndex(i);
                return;
            }
        }

        // If we get here, the instrument wasn't found in the combo
        logger.warn("Instrument with ID {} not found in combo for player {}",
                instrumentId, currentPlayer.getName());

        // Fall back to first item if available
        if (getItemCount() > 0) {
            setSelectedIndex(0);
        }
    }

    /**
     * Check if an instrument is appropriate for the specified channel
     */
    private boolean isInstrumentForChannel(InstrumentWrapper instrument, int playerChannel) {
        if (instrument == null) return false;

        // Check if instrument is on the same channel
        if (instrument.getChannel() != null && instrument.getChannel() == playerChannel) {
            return true;
        }

        // Check if instrument receives on multiple channels including this one
        if (instrument.getReceivedChannels() != null) {
            for (Integer ch : instrument.getReceivedChannels()) {
                if (ch != null && ch == playerChannel) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handle instrument selection change through PlayerInstrumentChangeEvent
     */
    private void handleSelectionChange() {
        if (currentPlayer == null || isInitializing || isUpdatingSelection) return;

        InstrumentWrapper selectedInstrument = (InstrumentWrapper) getSelectedItem();
        if (selectedInstrument == null) return;

        // Create a PlayerInstrumentChangeEvent instead of using the legacy request
        PlayerInstrumentChangeEvent event = new PlayerInstrumentChangeEvent(this, currentPlayer, selectedInstrument);
        CommandBus.getInstance().publish(Commands.PLAYER_INSTRUMENT_CHANGE_EVENT, this, event);

        logger.info("Instrument change published for player {} to {}",
                currentPlayer.getName(), selectedInstrument.getName());
    }

    /**
     * Select an instrument by ID without triggering further updates
     */
    private void selectInstrumentById(Long instrumentId) {
        if (instrumentId == null) return;

        isUpdatingSelection = true;
        try {
            for (int i = 0; i < getItemCount(); i++) {
                InstrumentWrapper instrument = getItemAt(i);
                if (instrument != null &&
                        instrument.getId() != null &&
                        instrument.getId().equals(instrumentId)) {
                    setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            isUpdatingSelection = false;
        }
    }
}
