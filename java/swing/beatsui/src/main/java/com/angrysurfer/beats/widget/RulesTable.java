package com.angrysurfer.beats.widget;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * A specialized JTable for displaying and managing Rule objects.
 * Provides functionality for selection, flashing, and command bus integration.
 */

@Getter
@Setter
public class RulesTable extends JTable implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(RulesTable.class.getName());

    private final RuleTableModel tableModel;
    private final Set<Long> flashingRuleIds = new HashSet<>();
    private final Color FLASH_COLOR = new Color(255, 220, 220); // Light red flash
    private final int FLASH_DURATION_MS = 500; // Flash duration in milliseconds
    private Timer flashTimer;
    private int lastSelectedRow = -1;

    public RulesTable() {
        this.tableModel = new RuleTableModel();
        setModel(tableModel);

        setupTable();
        setupSelectionListener();
        setupMouseListener();

        CommandBus.getInstance().register(this, new String[]{
                Commands.RULE_ADDED,
                Commands.RULE_EDITED,
                Commands.RULE_DELETED,
                Commands.PLAYER_SELECTION_EVENT
        });
    }

    private void setupTable() {
        // Configure appearance
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Set column widths
        getColumnModel().getColumn(0).setPreferredWidth(80); // Operator column wider
        getColumnModel().getColumn(1).setPreferredWidth(60); // Comparison
        getColumnModel().getColumn(2).setPreferredWidth(40); // Value
        getColumnModel().getColumn(3).setPreferredWidth(40); // Part

        // Apply custom renderer
        RulesRowRenderer rowRenderer = new RulesRowRenderer(this);
        for (int i = 0; i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }
    }

    private void setupSelectionListener() {
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (getSelectedRow() >= 0) {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null)
                        CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, selectedRule);
                } else CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this, null);
            }
        });
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        Rule rule = getSelectedRule();
                        if (rule != null) {
                            // Edit the rule on double-click
                            CommandBus.getInstance().publish(
                                    Commands.RULE_EDIT_REQUEST,
                                    this,
                                    rule
                            );
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.RULE_ADDED:
                if (action.getData() instanceof Rule rule) {
                    Player player = rule.getPlayer();
                    if (player != null) {
                        SwingUtilities.invokeLater(() -> {
                            // Refresh the table with the player's rules
                            tableModel.setRules(player.getRules());
                            // Flash the added rule
                            flashRuleById(rule.getId());
                            // Select the new rule
                            selectRuleById(rule.getId());
                        });
                    }
                }
                break;

            case Commands.RULE_EDITED:
                if (action.getData() instanceof Rule rule) {
                    Player player = rule.getPlayer();
                    if (player != null) {
                        SwingUtilities.invokeLater(() -> {
                            // Refresh the table with the player's rules
                            tableModel.setRules(player.getRules());
                            // Flash the edited rule
                            flashRuleById(rule.getId());
                            // Select the edited rule
                            selectRuleById(rule.getId());
                        });
                    }
                }
                break;

            case Commands.RULE_DELETED:
                SwingUtilities.invokeLater(() -> {
                    if (action.getData() instanceof Rule[] rules && rules.length > 0) {
                        Player player = rules[0].getPlayer();
                        if (player != null) {
                            // Refresh the table with the player's rules
                            tableModel.setRules(player.getRules());

                            // Select an appropriate row if there are any rules left
                            if (getRowCount() > 0) {
                                int rowToSelect = Math.min(lastSelectedRow, getRowCount() - 1);
                                if (rowToSelect >= 0) {
                                    setRowSelectionInterval(rowToSelect, rowToSelect);
                                }
                            }
                        }
                    }
                });
                break;

            case Commands.PLAYER_SELECTION_EVENT:
                if (action.getData() instanceof Player player) {
                    SwingUtilities.invokeLater(() -> {
                        // Update the table with the selected player's rules
                        if (player.getRules() != null) {
                            tableModel.setRules(player.getRules());
                        } else {
                            tableModel.setRules(Set.of());
                        }
                    });
                }
                break;
        }
    }

    /**
     * Get the rule at the selected row
     */
    public Rule getSelectedRule() {
        int row = getSelectedRow();
        if (row >= 0) {
            return tableModel.getRuleAt(row);
        }
        return null;
    }

    /**
     * Get all selected rules
     */
    public Rule[] getSelectedRules() {
        int[] selectedRows = getSelectedRows();
        return tableModel.getRulesAt(selectedRows);
    }

    /**
     * Select a rule by its ID
     */
    public void selectRuleById(Long ruleId) {
        if (ruleId == null) return;

        int row = tableModel.findRuleRowById(ruleId);
        if (row >= 0) {
            setRowSelectionInterval(row, row);
            scrollRectToVisible(getCellRect(row, 0, true));
            lastSelectedRow = row;
        }
    }

    /**
     * Flash a rule row by ID to highlight changes
     */
    public void flashRuleById(Long ruleId) {
        if (ruleId == null) return;

        // Add rule to flashing set
        flashingRuleIds.add(ruleId);

        // Cancel existing timer if one is running
        if (flashTimer != null && flashTimer.isRunning()) {
            flashTimer.stop();
        }

        // Create new timer to end the flash effect
        flashTimer = new Timer(FLASH_DURATION_MS, e -> {
            // Clear flashing rules
            flashingRuleIds.clear();
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

    /**
     * Check if a rule is currently flashing
     */
    public boolean isRuleFlashing(Rule rule) {
        return rule != null && rule.getId() != null && flashingRuleIds.contains(rule.getId());
    }

    /**
     * Check if a rule ID is currently flashing
     */
    public boolean isRuleFlashing(Long ruleId) {
        return flashingRuleIds.contains(ruleId);
    }

    /**
     * Get the flash color for use by renderers
     */
    public Color getFlashColor() {
        return FLASH_COLOR;
    }

}
