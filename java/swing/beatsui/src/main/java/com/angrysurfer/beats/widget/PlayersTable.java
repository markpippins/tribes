package com.angrysurfer.beats.widget;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayersTable extends JTable {
    private static final Logger logger = LoggerFactory.getLogger(PlayersTable.class.getName());
    private static final int[] BOOLEAN_COLUMNS = PlayersTableModel.getBooleanColumns();
    private static final int[] NUMERIC_COLUMNS = PlayersTableModel.getNumericColumns();
    private final PlayersTableModel tableModel;
    private final Set<Long> flashingPlayerIds = new HashSet<>();
    private final Color FLASH_COLOR = UIHelper.coolBlue; // new Color(255, 255, 200); // Light yellow flash
    private final int FLASH_DURATION_MS = 500; // Flash duration in milliseconds
    private Timer flashTimer;
    private int lastSelectedRow = -1;
    private ListSelectionListener selectionListener;

    public PlayersTable() {
        this.tableModel = new PlayersTableModel();
        setModel(tableModel);

        setupTable();
        setupSelectionListener();
        setupMouseListener();
        setupMouseWheel(); // Add this line
        setupCommandBusListener();
    }

    public PlayersTableModel getPlayersTableModel() {
        return tableModel;
    }

    private void setupTable() {
        // Modify the ID column to make it visible with reasonable width
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setMinWidth(60);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setMaxWidth(100);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_ID)).setPreferredWidth(80);

        // Set column widths for Name column
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME)).setMinWidth(100);
        getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME)).setPreferredWidth(150);

        // Double the width of the Instrument column
        int instrumentColumnIndex = tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT);
        getColumnModel().getColumn(instrumentColumnIndex).setMinWidth(120); // Increased from 100
        getColumnModel().getColumn(instrumentColumnIndex).setPreferredWidth(120); // Doubled from 150

        // Reduce the Note column back to normal size - no longer showing drum names
        // here
        int noteColumnIndex = tableModel.getColumnIndex(PlayersTableModel.COL_NOTE);
        getColumnModel().getColumn(noteColumnIndex).setMinWidth(40); // Reduced from 60
        getColumnModel().getColumn(noteColumnIndex).setPreferredWidth(60); // Reduced from 100
        getColumnModel().getColumn(noteColumnIndex).setMaxWidth(80); // Reduced from 160

        // Keep the wider Preset column to fit preset names and drum names
        int presetColumnIndex = tableModel.getColumnIndex(PlayersTableModel.COL_PRESET);
        getColumnModel().getColumn(presetColumnIndex).setMinWidth(80);
        getColumnModel().getColumn(presetColumnIndex).setPreferredWidth(100);
        getColumnModel().getColumn(presetColumnIndex).setMaxWidth(140);

        // Set up Owner column width
        int ownerColumnIndex = tableModel.getColumnIndex(PlayersTableModel.COL_OWNER);
        getColumnModel().getColumn(ownerColumnIndex).setMinWidth(80);
        getColumnModel().getColumn(ownerColumnIndex).setPreferredWidth(120);
        getColumnModel().getColumn(ownerColumnIndex).setMaxWidth(160);

        // Set fixed widths for other columns - skip Preset and Instrument columns
        for (int i = 2; i < getColumnCount(); i++) {
            if (i != presetColumnIndex && i != instrumentColumnIndex && i != noteColumnIndex) {
                getColumnModel().getColumn(i).setMaxWidth(80);
                getColumnModel().getColumn(i).setPreferredWidth(60);
            }
        }

        // Configure table appearance
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setAutoCreateRowSorter(true);

        // Set default checkbox renderer for Boolean columns
        setupBooleanColumnRenderers();

        // Add left alignment for header columns
        setupHeaderRenderers();

        // Add column reordering listener
        setupColumnReorderingListener();

        // Save and restore column order
        SwingUtilities.invokeLater(
                () -> UIHelper.saveColumnOrder(this, Constants.PLAYER, PlayersTableModel.COLUMNS));
        SwingUtilities.invokeLater(
                () -> UIHelper.restoreColumnOrder(this, Constants.PLAYER, PlayersTableModel.COLUMNS));

        // Set custom renderer for all rows - this handles centering numeric values
        // internally
        setupCustomRowRenderer();
    }

    private void setupBooleanColumnRenderers() {
        for (int booleanColumn : BOOLEAN_COLUMNS) {
            getColumnModel().getColumn(booleanColumn).setCellRenderer(
                    new DefaultTableCellRenderer() {
                        private final JCheckBox checkbox = new JCheckBox();

                        {
                            checkbox.setHorizontalAlignment(JCheckBox.CENTER);
                        }

                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value,
                                                                       boolean isSelected, boolean hasFocus, int row, int column) {
                            if (value instanceof Boolean) {
                                checkbox.setSelected((Boolean) value);

                                // Determine background color
                                Color bgColor = table.getBackground();
                                Player player = getPlayerAtRow(row);

                                if (isPlayerFlashing(player)) {
                                    bgColor = isSelected ? FLASH_COLOR.darker() : FLASH_COLOR;
                                } else if (player != null && player.isPlaying()) {
                                    bgColor = isSelected ? UIHelper.mutedRed.darker() : UIHelper.fadedLime;
                                } else if (isSelected) {
                                    bgColor = table.getSelectionBackground();
                                }

                                checkbox.setBackground(bgColor);
                                return checkbox;
                            }
                            return super.getTableCellRendererComponent(table, value, isSelected,
                                    hasFocus, row, column);
                        }
                    });
        }
    }

    private void setupHeaderRenderers() {
        DefaultTableCellRenderer leftHeaderRenderer = new DefaultTableCellRenderer();
        leftHeaderRenderer.setHorizontalAlignment(JLabel.LEFT);

        getTableHeader().getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_NAME))
                .setHeaderRenderer(leftHeaderRenderer);
        getTableHeader().getColumnModel().getColumn(tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT))
                .setHeaderRenderer(leftHeaderRenderer);
    }

    private void setupColumnReorderingListener() {
        getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnMoved(TableColumnModelEvent e) {
                if (e.getFromIndex() != e.getToIndex()) {
                    logger.info("Column moved from " + e.getFromIndex() + " to " + e.getToIndex());
                    SwingUtilities.invokeLater(
                            () -> UIHelper.saveColumnOrder(PlayersTable.this, Constants.PLAYER,
                                    PlayersTableModel.COLUMNS));
                }
            }

            public void columnAdded(TableColumnModelEvent e) {
            }

            public void columnRemoved(TableColumnModelEvent e) {
            }

            public void columnMarginChanged(ChangeEvent e) {
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });
    }

    private void setupCustomRowRenderer() {
        PlayerRowRenderer rowRenderer = new PlayerRowRenderer(this);
        for (int i = 0; i < getColumnCount(); i++) {
            if (!isInArray(BOOLEAN_COLUMNS, i)) {
                getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
            }
        }
    }

    private void setupSelectionListener() {
        selectionListener = e -> {
            if (!e.getValueIsAdjusting()) { // Only handle when selection is complete
                int selectedRow = getSelectedRow();
                handlePlayerSelection(selectedRow);
            }
        };
        getSelectionModel().addListSelectionListener(selectionListener);
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        Player player = getPlayerAtRow(row);
                        if (player != null) {
                            // Edit the player on double-click
                            logger.info("Double-clicked player: " + player.getName());
                            CommandBus.getInstance().publish(Commands.PLAYER_EDIT_REQUEST, this, player);
                        }
                    }
                }
            }
        });
    }

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.PLAYER_ADDED:
                        if (action.getData() instanceof Player player) {
                            logger.info("PlayersTable received PLAYER_ADDED: {}", player.getName());

                            SwingUtilities.invokeLater(() -> {
                                // Only add if not already in table
                                if (findPlayerRowIndex(player) == -1) {
                                    getPlayersTableModel().addPlayerRow(player);
                                    logger.info("Added player to table model: {}", player.getName());
                                    repaint();
                                }
                            });
                        }
                        break;

                    case Commands.PLAYER_DELETED:
                        if (action.getData() instanceof Player player) {
                            logger.info("PlayersTable received PLAYER_DELETED: {}", player.getName());

                            SwingUtilities.invokeLater(() -> {
                                int rowIndex = findPlayerRowIndex(player);
                                if (rowIndex >= 0) {
                                    logger.info("Removing player from table row {}: {}", rowIndex, player.getName());
                                    getPlayersTableModel().removeRow(convertRowIndexToModel(rowIndex));
                                    repaint();
                                }
                            });
                        }
                        break;

                    case Commands.PLAYER_ROW_REFRESH:
                        if (action.getData() instanceof Player player) {
                            SwingUtilities.invokeLater(() -> {
                                updatePlayerRow(player);
                            });
                        }
                        break;

                    case Commands.SESSION_UPDATED:
                    case Commands.SESSION_SELECTED:
                    case Commands.SESSION_LOADED:
                        if (action.getData() instanceof Session session) {
                            SwingUtilities.invokeLater(() -> {
                                updateTableFromSession(session);
                            });
                        }
                        break;

                    case Commands.NEW_VALUE_VELOCITY_MIN:
                    case Commands.NEW_VALUE_VELOCITY_MAX:
                        if (action.getData() instanceof Object[] data && data.length >= 2) {
                            if (data[0] instanceof Long playerId && data[1] instanceof Long value) {
                                SwingUtilities.invokeLater(() -> {
                                    // Find player in table
                                    int rowIndex = findPlayerRowIndexById(playerId);
                                    if (rowIndex >= 0) {
                                        // Get player
                                        Player player = getPlayerAtRow(rowIndex);
                                        if (player != null) {
                                            // Update table row
                                            updatePlayerRow(player);
                                        }
                                    }
                                });
                            }
                        }
                        break;

                    case Commands.PLAYER_SELECTION_EVENT:
                        if (action.getData() instanceof Player player) {
                            SwingUtilities.invokeLater(() -> {
                                // Find the row for this player
                                int rowIndex = findPlayerRowIndex(player);
                                if (rowIndex >= 0) {
                                    // Select the row without triggering additional selection events
                                    getSelectionModel().removeListSelectionListener(selectionListener);

                                    // Clear current selection and select the player's row
                                    clearSelection();
                                    setRowSelectionInterval(rowIndex, rowIndex);

                                    // Make sure the row is visible
                                    scrollRectToVisible(getCellRect(rowIndex, 0, true));

                                    // Store as last selected row
                                    lastSelectedRow = rowIndex;

                                    // Restore the selection listener
                                    getSelectionModel().addListSelectionListener(selectionListener);

                                    // Request focus so keyboard navigation works
                                    requestFocus();

                                    logger.info("Selected row " + rowIndex + " for player: " + player.getName());
                                }
                            });
                        }
                        break;
                }
            }
        }, new String[]{
                Commands.PLAYER_ADDED,
                Commands.PLAYER_DELETED,
                Commands.PLAYER_ROW_REFRESH,
                Commands.SESSION_UPDATED,
                Commands.SESSION_SELECTED,
                Commands.SESSION_LOADED,
                Commands.NEW_VALUE_VELOCITY_MIN,
                Commands.NEW_VALUE_VELOCITY_MAX,
                Commands.PLAYER_SELECTION_EVENT
        });
    }

    private void updateTableFromSession(Session session) {
        // Clear existing rows
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }

        // Add all players from the session
        if (session != null && session.getPlayers() != null) {
            for (Player player : session.getPlayers()) {
                tableModel.addPlayerRow(player);
            }
        }

        // Sort and repaint
        sortTable();
        repaint();
    }

    public void handlePlayerSelection(int row) {
        if (row >= 0) {
            lastSelectedRow = row;
        }

        try {
            Player player = null;

            if (row >= 0) {
                player = getPlayerAtRow(row);
            }

            if (player != null) {
                CommandBus.getInstance().publish(Commands.PLAYER_SELECTION_EVENT, this, player);
            } else {
                // CommandBus.getInstance().publish(Commands.PLAYER_UNSELECTED, this, null);
            }
        } catch (Exception ex) {
            logger.error("Error in player selection", ex);
        }
    }

    public Player getPlayerAtRow(int row) {
        if (row < 0 || tableModel.getRowCount() <= row) {
            return null;
        }

        try {
            // Convert view index to model index in case of sorting/filtering
            int modelRow = convertRowIndexToModel(row);

            // Get the player ID from the ID column
            Long playerId = (Long) tableModel.getValueAt(
                    modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_ID));

            // Get the current session
            Session currentSession = SessionManager.getInstance().getActiveSession();

            if (currentSession != null && currentSession.getPlayers() != null) {
                // Find the player with the matching ID
                return currentSession.getPlayers().stream()
                        .filter(p -> playerId.equals(p.getId()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            logger.error("Error getting player at row", e);
        }

        return null;
    }

    public void flashPlayerRow(Player player) {
        if (player == null || player.getId() == null) {
            return;
        }

        // Add player to flashing set
        flashingPlayerIds.add(player.getId());

        // Cancel existing timer if one is running
        if (flashTimer != null && flashTimer.isRunning()) {
            flashTimer.stop();
        }

        // Create new timer to end the flash effect
        flashTimer = new Timer(FLASH_DURATION_MS, e -> {
            // Clear flashing players
            flashingPlayerIds.clear();

            // Repaint the table
            repaint();

            // Stop the timer
            ((Timer) e.getSource()).stop();
        });

        // Start the timer
        flashTimer.setRepeats(false);
        flashTimer.start();

        // Immediately repaint to show flash
        repaint();
    }

    public boolean isPlayerFlashing(Player player) {
        return player != null && player.getId() != null &&
                flashingPlayerIds.contains(player.getId());
    }

    public boolean isPlayerFlashing(Long playerId) {
        return flashingPlayerIds.contains(playerId);
    }

    private boolean isInArray(int[] array, int value) {
        for (int i : array) {
            if (i == value)
                return true;
        }
        return false;
    }

    public void updatePlayerRow(Player player) {
        if (player == null)
            return;

        try {
            // Find row index for this player
            int rowIndex = findPlayerRowIndex(player);
            if (rowIndex == -1) {
                logger.error("Player not found in table: " + player.getName());
                return;
            }

            // Update all cells in the row
            int modelRow = convertRowIndexToModel(rowIndex);

            // Update each column with fresh data
            tableModel.setValueAt(player.getName(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_NAME));
            tableModel.setValueAt(player.getRootNote(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_NOTE));
            tableModel.setValueAt(player.getLevel(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_LEVEL));
            tableModel.setValueAt(player.isMuted(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_MUTE));
            tableModel.setValueAt(player.getProbability(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PROBABILITY));
            tableModel.setValueAt(player.getSparse(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_SPARSE));
            tableModel.setValueAt(player.getSwing(), modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_SWING));
            tableModel.setValueAt(player.getRandomDegree(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_RANDOM));
            tableModel.setValueAt(player.getMinVelocity(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_MIN_VEL));
            tableModel.setValueAt(player.getMaxVelocity(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_MAX_VEL));
            tableModel.setValueAt(player.getPreset(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PRESET));
            tableModel.setValueAt(player.getPanPosition(), modelRow,
                    tableModel.getColumnIndex(PlayersTableModel.COL_PAN));

            // Update owner display
            String ownerName = "None";
            if (player.getOwner() != null) {
                ownerName = player.getOwner().getClass().getSimpleName();
            }
            tableModel.setValueAt(ownerName, modelRow, tableModel.getColumnIndex(PlayersTableModel.COL_OWNER));

        // Special handling for instrument column
        @SuppressWarnings("unchecked")
        java.util.Vector<Object> rowVector = (java.util.Vector<Object>) tableModel.getDataVector().get(modelRow);
        tableModel.updateInstrumentCell(rowVector,
            tableModel.getColumnIndex(PlayersTableModel.COL_INSTRUMENT), player);

            // Notify the model that data has changed
            tableModel.fireTableRowsUpdated(modelRow, modelRow);

            // Flash the row to indicate update
            flashPlayerRow(player);

            logger.info("Updated row " + rowIndex + " for player: " + player.getName());
        } catch (Exception e) {
            logger.error("Error updating player row", e);
        }
    }

    public int findPlayerRowIndex(Player player) {
        PlayersTableModel model = getPlayersTableModel();
        if (player == null || model == null)
            return -1;

        for (int i = 0; i < model.getRowCount(); i++) {
            Long rowPlayerId = (Long) model.getValueAt(i, 0);
            if (rowPlayerId != null && rowPlayerId.equals(player.getId())) {
                return convertRowIndexFromModel(i);
            }
        }
        return -1;
    }

    /**
     * Find the view row index of a player by ID
     *
     * @param playerId ID of the player to find
     * @return Row index in view coordinates, or -1 if not found
     */
    public int findPlayerRowIndexById(Long playerId) {
        PlayersTableModel model = getPlayersTableModel();
        if (playerId == null || model == null)
            return -1;

        // Search through model rows
        for (int i = 0; i < model.getRowCount(); i++) {
            Long rowPlayerId = (Long) model.getValueAt(i, model.getColumnIndex(PlayersTableModel.COL_ID));
            if (rowPlayerId != null && rowPlayerId.equals(playerId)) {
                // Convert to view coordinates
                return convertRowIndexFromModel(i);
            }
        }

        return -1;
    }

    public int getLastSelectedRow() {
        return lastSelectedRow;
    }

    public void setLastSelectedRow(int row) {
        this.lastSelectedRow = row;
    }

    /**
     * Forwards column index lookup to the table model
     */
    public int getColumnIndex(String columnName) {
        return getPlayersTableModel().getColumnIndex(columnName);
    }

    /**
     * Get the flash color for use by renderers
     */
    public Color getFlashColor() {
        return FLASH_COLOR;
    }

    /**
     * Get the flash duration for use by renderers
     */
    public int getFlashDurationMs() {
        return FLASH_DURATION_MS;
    }

    /**
     * Sorts the table by player name
     */
    public void sortTable() {
        // Fix the unchecked cast warning
        if (getRowSorter() instanceof TableRowSorter<?> sorter) {
            int nameColumnIndex = getColumnIndex(PlayersTableModel.COL_NAME);
            List<SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new SortKey(nameColumnIndex, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            sorter.sort();
        }
    }

    /**
     * Converts a row index from the model's coordinate space to the view's
     * coordinate space
     *
     * @param modelRow The row index in model coordinates
     * @return The row index in view coordinates, or -1 if not found/visible
     */
    public int convertRowIndexFromModel(int modelRow) {
        if (modelRow < 0) {
            return -1;
        }

        // Search through all rows in the view to find the one that maps to this model
        // row
        for (int i = 0; i < getRowCount(); i++) {
            if (convertRowIndexToModel(i) == modelRow) {
                return i;
            }
        }

        // Row might not be visible due to filtering
        return -1;
    }

    private void setupMouseWheel() {
        addMouseWheelListener((java.awt.event.MouseWheelEvent e) -> {
            // Get the parent scroll pane if available
            java.awt.Container parent = getParent();
            while (parent != null && !(parent instanceof javax.swing.JScrollPane)) {
                parent = parent.getParent();
            }

            if (parent != null) {
                // We have a scroll pane, let's scroll it
                javax.swing.JScrollPane scrollPane = (javax.swing.JScrollPane) parent;

                // Get the current scroll position
                int currentPosition = scrollPane.getVerticalScrollBar().getValue();

                // Calculate scroll amount - faster when modifier keys are pressed
                int scrollAmount = e.getUnitsToScroll() * getRowHeight();
                if (e.isShiftDown()) {
                    scrollAmount *= 3; // Scroll 3x faster with shift
                }
                if (e.isControlDown()) {
                    scrollAmount *= 5; // Scroll 5x faster with control
                }

                // Apply the new scroll position
                scrollPane.getVerticalScrollBar().setValue(currentPosition + scrollAmount);

                // Consume the event so it doesn't propagate
                e.consume();
            }
        });
    }
}
