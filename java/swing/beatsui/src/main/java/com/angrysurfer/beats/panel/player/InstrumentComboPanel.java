package com.angrysurfer.beats.panel.player;

import com.angrysurfer.beats.panel.LivePanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.AddInstrumentButton;
import com.angrysurfer.beats.widget.InstrumentCombo;
import com.angrysurfer.core.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing an InstrumentCombo with proper PlayerAwarePanel integration
 */
public class InstrumentComboPanel extends LivePanel {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentComboPanel.class);

    // UI components
    private InstrumentCombo combo;

    public InstrumentComboPanel() {
        super();
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        UIHelper.setWidgetPanelBorder(this, "Instrument");

        initializeComponents();
        setMaximumSize(new Dimension(getPreferredSize().width, getPreferredSize().height));
    }

    @Override
    public void handlePlayerActivated() {
        Player player = getPlayer();
        if (player != null) {
            logger.debug("Activating player in InstrumentComboPanel: {}", player.getName());

            // Update the panel title with player name
            UIHelper.setWidgetPanelBorder(this, player.getName());

            // Set the player in the combo box - this will refresh the instruments
            combo.setCurrentPlayer(player);
        } else {
            logger.warn("Null player activated in InstrumentComboPanel");
            UIHelper.setWidgetPanelBorder(this, "Instrument");
        }
    }

    @Override
    public void handlePlayerUpdated() {
        Player player = getPlayer();
        if (player != null) {
            logger.debug("Player updated: {}", player.getName());

            // Update title if needed
            UIHelper.setWidgetPanelBorder(this, player.getName());

            // Only update combo if the player's instrument has changed
            if (combo.getCurrentPlayer() == null ||
                    combo.getCurrentPlayer().getInstrumentId() == null ||
                    player.getInstrumentId() == null ||
                    !player.getInstrumentId().equals(combo.getCurrentPlayer().getInstrumentId())) {
                updateComboFromPlayer(player);
            }
        }
    }

    /**
     * Update the combo box based on the player information
     */
    private void updateComboFromPlayer(Player player) {
        if (player == null || combo == null) return;

        // Schedule update on EDT for thread safety
        SwingUtilities.invokeLater(() -> {
            // First check if player is already set as current
            if (combo.getCurrentPlayer() == null ||
                    !player.getId().equals(combo.getCurrentPlayer().getId())) {
                // Different player, set it as current
                combo.setCurrentPlayer(player);
            } else if (player.getInstrumentId() != null) {
                // Same player but instrument might have changed
                // Only update if the player's instrument ID has changed
                if (combo.getCurrentPlayer().getInstrumentId() == null ||
                        !player.getInstrumentId().equals(combo.getCurrentPlayer().getInstrumentId())) {
                    logger.debug("Instrument changed for player {}: {} -> {}",
                            player.getName(),
                            combo.getCurrentPlayer().getInstrumentId(),
                            player.getInstrumentId());
                    combo.updateSelectedInstrument(player);
                }
            }
        });
    }

    /**
     * Initialize UI components with standardized sizing
     */
    private void initializeComponents() {
        // Create instrument combo
        combo = new InstrumentCombo();

        // Initialize with current player if available
        Player player = getPlayer();
        if (player != null) {
            combo.setCurrentPlayer(player);
            UIHelper.setWidgetPanelBorder(this, player.getName());
        }

        add(combo);
        add(new AddInstrumentButton());
    }

    /**
     * Public method to manually refresh the combo
     */
    public void refreshInstruments() {
        if (combo != null) {
            Player player = getPlayer();
            if (player != null) {
                combo.setCurrentPlayer(player);
            }
        }
    }

    /**
     * Get the selected instrument from the combo
     */
    public Object getSelectedInstrument() {
        return combo != null ? combo.getSelectedItem() : null;
    }
}
