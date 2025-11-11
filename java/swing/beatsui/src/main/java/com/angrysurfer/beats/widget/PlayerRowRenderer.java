package com.angrysurfer.beats.widget;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.SoundbankService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom renderer for player table rows that centers numeric values
 */
public class PlayerRowRenderer extends DefaultTableCellRenderer implements IBusListener {
    private static final long serialVersionUID = 1L;
    private static final Color PLAYING_COLOR = new Color(255, 165, 0); // Bright orange for better visibility
    private final PlayersTable table;
    // Track the current session note offset
    private int sessionNoteOffset = 0;

    // Update the constructor 
    public PlayerRowRenderer(PlayersTable table) {
        this.table = table;

        // Register for specific command bus events
        CommandBus.getInstance().register(this, new String[] {
            Commands.SESSION_UPDATED,
            Commands.SESSION_SELECTED,
            Commands.TRANSPOSE_UP,
            Commands.TRANSPOSE_DOWN
        });

        // Initialize with current session's offset if available
        Session currentSession = SessionManager.getInstance().getActiveSession();
        if (currentSession != null && currentSession.getNoteOffset() != null) {
            sessionNoteOffset = currentSession.getNoteOffset();
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        // Listen for session updates and transpose commands
        switch (action.getCommand()) {
            case Commands.SESSION_UPDATED:
            case Commands.SESSION_SELECTED:
                if (action.getData() instanceof Session session && session.getNoteOffset() != null) {
                    sessionNoteOffset = session.getNoteOffset();
                    table.repaint(); // Repaint the table to refresh drum names
                }
                break;

            case Commands.TRANSPOSE_UP:
                sessionNoteOffset++;
                table.repaint();
                break;

            case Commands.TRANSPOSE_DOWN:
                sessionNoteOffset--;
                table.repaint();
                break;
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable ignored, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // Use our table reference instead of the provided one
        Component c = super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);

        if (c instanceof JLabel label) {
            Player player = table.getPlayerAtRow(row);

            if (player == null) {
                return c;
            }

            // Check if this is a numeric column (using column model index)
            int modelColumnIndex = table.getColumnModel().getColumn(column).getModelIndex();

            // Get numeric columns array from the table model
            int[] numericColumns = PlayersTableModel.getNumericColumns();

            // Check if this is the Preset column specifically
            boolean isPresetColumn = modelColumnIndex == table.getColumnIndex(PlayersTableModel.COL_PRESET);

            // Special handling for preset column
            if (isPresetColumn) {
                // For drum channel (channel 9), show drum name instead of preset
                if (player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL) {
                    // Get the current note value from the player WITH offset applied
                    int noteValue = player.getRootNote() != null ? player.getRootNote().intValue() : 0;

                    // Apply session offset to get the actual note being played
                    int actualNote = noteValue + sessionNoteOffset;

                    // Keep note in valid MIDI range (0-127)
                    actualNote = Math.max(0, Math.min(127, actualNote));

                    // Get drum name from InternalSynthManager using the actual note
                    String drumName = MidiService.getInstance().getDrumName(actualNote);

                    // Show drum name in the preset column
                    label.setText(drumName);
                    label.setToolTipText("Drum: " + drumName + " (Note: " + actualNote + ")");
                    label.setHorizontalAlignment(JLabel.LEFT); // Left-align drum names as they can be long
                }
                // For instruments with loaded soundbanks, show preset names from that soundbank
                else if (player.getInstrument() != null && player.getInstrument().getSoundbankName() != null) {
                    // Get preset value
                    long presetValue = value instanceof Number ? ((Number) value).longValue() : 0;

                    // Get bank index (default to 0 if null)
                    int bankIndex = player.getInstrument().getBankIndex() != null ?
                            player.getInstrument().getBankIndex() : 0;

                    // Get preset name using soundbank name and bank
                    String presetName = SoundbankService.getInstance().getPresetNames(
                                    player.getInstrument().getSoundbankName(), bankIndex)
                            .stream()
                            .skip(presetValue)
                            .findFirst()
                            .orElse("Preset " + presetValue);

                    // Use preset name instead of number
                    label.setText(presetName);

                    // Enhanced tooltip with better formatting
                    String tooltip = String.format("<html>" +
                                    "<b>Soundbank:</b> %s<br>" +
                                    "<b>Bank:</b> %d<br>" +
                                    "<b>Preset %d:</b> %s" +
                                    "</html>",
                            player.getInstrument().getSoundbankName(),
                            bankIndex,
                            presetValue,
                            presetName);

                    label.setToolTipText(tooltip);
                    label.setHorizontalAlignment(JLabel.LEFT);
                }
                // For internal synth instruments, show preset name
                else if (player.getInstrument() != null &&
                        MidiService.getInstance().isInternalSynth(player.getInstrument())) {
                    // Get preset name from InternalSynthManager
                    String presetName = SoundbankService.getInstance().getPresetName(
                            player.getInstrument().getId(),
                            value instanceof Number ? ((Number) value).longValue() : 0
                    );

                    // Use preset name instead of number
                    label.setText(presetName);
                    label.setToolTipText(presetName);
                    label.setHorizontalAlignment(JLabel.LEFT);
                }
                // For standard presets, just show the number
                else {
                    label.setHorizontalAlignment(JLabel.CENTER); // Keep numeric presets centered
                }

                // Set background color based on player state
                setBackgroundColor(label, player, isSelected);
                return c;
            }

            // Special handling for Note column to show actual note with offset
            boolean isNoteColumn = modelColumnIndex == table.getColumnIndex(PlayersTableModel.COL_NOTE);
            if (isNoteColumn && sessionNoteOffset != 0) {
                if (value instanceof Number) {
                    int noteValue = ((Number) value).intValue();
                    int actualNote = noteValue + sessionNoteOffset;

                    // Format as "base (actual)" when offset is applied
                    label.setText(noteValue + " → " + actualNote);
                    label.setToolTipText("Base note: " + noteValue + ", With offset: " + actualNote);
                }
            }

            // Add special handling for Owner column - highlight different owner types with colors
            boolean isOwnerColumn = modelColumnIndex == table.getColumnIndex(PlayersTableModel.COL_OWNER);
            if (isOwnerColumn && value instanceof String ownerType) {

                // Set a tooltip with more info
                label.setToolTipText("Owner: " + ownerType);

                // Color code different owner types
                if ("DrumSequencer".equals(ownerType)) {
                    label.setForeground(isSelected ? Color.WHITE : new Color(0, 128, 0)); // Dark green
                } else if ("MelodicSequencer".equals(ownerType)) {
                    label.setForeground(isSelected ? Color.WHITE : new Color(0, 0, 160)); // Dark blue
                } else if (ownerType.contains("Sequencer")) {
                    label.setForeground(isSelected ? Color.WHITE : new Color(128, 0, 128)); // Purple
                }
            }

            // For all other columns, handle numeric vs non-numeric formatting
            boolean isNumeric = false;
            for (int numericCol : numericColumns) {
                if (numericCol == modelColumnIndex) {
                    isNumeric = true;
                    break;
                }
            }

            if (isNumeric) {
                label.setHorizontalAlignment(JLabel.CENTER);
            } else {
                label.setHorizontalAlignment(JLabel.LEFT);
            }

            // Set background color based on player state
            setBackgroundColor(label, player, isSelected);
        }

        return c;
    }

    // Helper method to set background color based on player state
    private void setBackgroundColor(JLabel label, Player player, boolean isSelected) {
        if (player != null) {
            // First check if this player is flashing (priority over other states)
            if (table.isPlayerFlashing(player)) {
                label.setBackground(isSelected ? table.getFlashColor().darker() : table.getFlashColor());
                label.setForeground(Color.BLACK);
            }
            // Then check if playing
            else if (player.isPlaying()) {
                label.setBackground(isSelected ? PLAYING_COLOR.darker() : PLAYING_COLOR);
                label.setForeground(Color.BLACK);
            }
            // Default colors
            else {
                label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            }
        }
    }
}
