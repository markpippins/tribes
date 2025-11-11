package com.angrysurfer.beats.widget;

import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Player;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * A button for adding new instruments and directly assigning them to the active player
 */
@Getter
@Setter
public class AddInstrumentButton extends JButton implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(AddInstrumentButton.class);

    private Player currentPlayer;

    /**
     * Create a new Add Instrument button
     */
    public AddInstrumentButton() {
        super("+");

        // Set appropriate styling
        setToolTipText("Create a new instrument for this player");
        setMargin(new Insets(1, 4, 1, 4));
        setPreferredSize(new Dimension(24, 24));

        // Register for events
        CommandBus.getInstance().register(this, new String[]{Commands.PLAYER_SELECTION_EVENT});

        // Add action handler
        addActionListener(e -> handleButtonClick());
    }

    /**
     * Create with an initial player
     */
    public AddInstrumentButton(Player player) {
        this();
        this.currentPlayer = player;
    }

    /**
     * Handle button click - request instrument creation dialog via command bus
     */
    private void handleButtonClick() {
        if (currentPlayer == null) {
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("AddInstrumentButton", "Warning", "No player selected")
            );
            return;
        }

        // Request the dialog via command bus - the DialogManager will handle it
        CommandBus.getInstance().publish(Commands.CREATE_INSTRUMENT_FOR_PLAYER_REQUEST, this, currentPlayer);
    }

    /**
     * Handle command bus events - primarily player selection
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        if (action.getCommand().equals(Commands.PLAYER_SELECTION_EVENT)) {
            if (action.getData() instanceof Player player) {
                currentPlayer = player;
                setEnabled(true);
            }
        }
    }
}
