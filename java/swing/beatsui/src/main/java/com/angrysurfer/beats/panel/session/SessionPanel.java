package com.angrysurfer.beats.panel.session;

import com.angrysurfer.beats.panel.ControlPanel;
import com.angrysurfer.beats.panel.PianoPanel;
import com.angrysurfer.beats.panel.player.PlayerTimelinePanel;
import com.angrysurfer.beats.panel.player.PlayersPanel;
import com.angrysurfer.beats.panel.player.RulesPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Objects;

// Update the SessionPanel class to use the new ControlPanel
@Getter
@Setter
public class SessionPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionPanel.class.getName());
    private static final long EVENT_THROTTLE_MS = 100;
    private static Long lastProcessedSessionId = null;
    private final PlayersPanel playerTablePanel;
    private final RulesPanel ruleTablePanel;
    private final ControlPanel controlPanel;
    private final PianoPanel pianoPanel;
    private final PlayerTimelinePanel playerTimelinePanel;
    private long lastSessionEventTime = 0;

    public SessionPanel() {
        super(new BorderLayout());

        // Initialize panels and pass this reference for callbacks
        this.ruleTablePanel = new RulesPanel();
        this.playerTablePanel = new PlayersPanel();
        this.controlPanel = new ControlPanel();
        this.pianoPanel = new PianoPanel();
        this.playerTimelinePanel = new PlayerTimelinePanel();
        setupComponents();

        // Register for specific events only
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_SELECTION_EVENT,
                Commands.SESSION_UPDATED,
                Commands.SESSION_CHANGED,
                Commands.SESSION_SELECTED
        });
    }

    private void setupComponents() {
        setLayout(new BorderLayout());

        // Create container for tables with BorderLayout instead of JSplitPane
        JPanel tablesPanel = new JPanel(new BorderLayout());

        // Add player table to CENTER (will take all available space)
        tablesPanel.add(playerTablePanel, BorderLayout.CENTER);

        // Make the rules panel skinny with preferred width
        ruleTablePanel.setPreferredSize(new Dimension(220, ruleTablePanel.getPreferredSize().height));

        // Add rule table to EAST (will take minimum space needed)
        tablesPanel.add(ruleTablePanel, BorderLayout.EAST);

        // Create the bottom panel with proper constraints
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Piano and control panel - create with FIXED height
        JPanel controlContainerPanel = new JPanel(new BorderLayout());
        controlContainerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Minimal padding

        // Set fixed height for control panel - increased from 90 to 100px
        int controlHeight = 100;
        controlContainerPanel.setPreferredSize(new Dimension(800, controlHeight));
        controlContainerPanel.setMinimumSize(new Dimension(200, controlHeight));
        controlContainerPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlHeight));

        // Add components to control container
        controlContainerPanel.add(pianoPanel, BorderLayout.WEST);
        controlContainerPanel.add(controlPanel, BorderLayout.CENTER);

        // Create timeline container with fixed height
        JPanel timelineContainer = new JPanel(new BorderLayout());
        timelineContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // NO padding

        // Calculate height needed with doubled header height
        int timelineHeight = 140;
        playerTimelinePanel.setPreferredSize(new Dimension(800, timelineHeight));
        playerTimelinePanel.setMinimumSize(new Dimension(200, timelineHeight));
        playerTimelinePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, timelineHeight));

        // Add timeline to container
        timelineContainer.add(playerTimelinePanel, BorderLayout.CENTER);

        // Use BorderLayout for fixed heights
        JPanel combinedPanel = new JPanel(new BorderLayout());
        combinedPanel.add(timelineContainer, BorderLayout.CENTER);
        combinedPanel.add(controlContainerPanel, BorderLayout.SOUTH);

        // Add combined panel to the bottom panel
        bottomPanel.add(combinedPanel, BorderLayout.CENTER);

        // Use BorderLayout for main panel
        add(tablesPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Component listener for resizing - modify to handle new layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Set the bottom panel height to exactly what's needed for timeline + control
                int requiredHeight = timelineHeight + controlHeight + 50; // adding extra for borders/insets
                bottomPanel.setPreferredSize(new Dimension(getWidth(), requiredHeight));
                revalidate();
            }
        });

        // Remove the divider location code for the tableSplitPane since it no longer exists
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null || action.getSender() == this) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.PLAYER_SELECTION_EVENT:
                handlePlayerSelected((Player) action.getData());
                break;
            // case Commands.PLAYER_UNSELECTED:
            //     handlePlayerUnselected();
            //     break;
            // Add handlers for session changes
            case Commands.SESSION_UPDATED:
            case Commands.SESSION_CHANGED:
            case Commands.SESSION_SELECTED:
                if (action.getData() instanceof Session session) {
                    // Add throttling check
                    long now = System.currentTimeMillis();
                    Long sessionId = session.getId();

                    // Check if we've already processed this exact session recently
                    if (sessionId != null &&
                            sessionId.equals(lastProcessedSessionId) &&
                            (now - lastSessionEventTime) < EVENT_THROTTLE_MS) {
                        logger.debug("SessionPanel: Ignoring duplicate session event: {}", sessionId);
                        return;
                    }

                    lastProcessedSessionId = sessionId;
                    lastSessionEventTime = now;

                    logger.info("SessionPanel received session update: {}", sessionId);

                    // Use SwingUtilities.invokeLater to break potential call stack loops
                    final Session finalSession = session;
                    SwingUtilities.invokeLater(() -> {
                        updateSessionDisplay(finalSession);
                    });
                }
                break;
            default:
                break;
        }
    }

    private void handlePlayerUnselected() {
        if (Objects.nonNull(this.playerTimelinePanel)) {
            this.playerTimelinePanel.setPlayer(null);
        }
    }

    private void handlePlayerSelected(Player player) {
        if (Objects.nonNull(this.playerTimelinePanel)) {
            // Set the player data
            this.playerTimelinePanel.setPlayer(player);

            // Simply refresh the panel - no JScrollPane manipulation needed
            SwingUtilities.invokeLater(() -> {
                // We've removed the JScrollPane, so just update the panel directly
                playerTimelinePanel.revalidate();
                playerTimelinePanel.repaint();
            });
        }
    }

    // Update the updateSessionDisplay method
    private void updateSessionDisplay(Session session) {
        if (session == null) {
            logger.warn("Attempted to update session display with null session");
            return;
        }

        logger.info("Updating session display for session: {}", session.getId());

        // Just update this panel's own components - don't try to update other panels
        // that already listen to the same events

        // Force a repaint of this panel
        revalidate();
        repaint();
    }
}
