package com.angrysurfer.beats.visualization.handler.music;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.LockHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScrollingSequencerVisualization extends LockHandler implements IVisualizationHandler, IBusListener {
    // Frame rate for visual updates (60fps for smoother animation)
    private static final int FRAMES_PER_SECOND = 60;

    // Updated colors as requested
    private static final Color BEAT_MARKER_COLOR = UIHelper.charcoalGray;
    private static final Color BAR_MARKER_COLOR = Color.BLUE;
    private static final Color POSITION_INDICATOR = Color.WHITE;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    // Add new colors for player visualization
    private static final Color[] PLAYER_COLORS = {
            new Color(220, 0, 0),      // Red
            new Color(0, 220, 0),      // Green
            new Color(0, 0, 220),      // Blue
            new Color(220, 220, 0),    // Yellow
            new Color(220, 0, 220),    // Magenta
            new Color(0, 220, 220),    // Cyan
            new Color(255, 128, 0),    // Orange
            new Color(128, 0, 255),    // Purple
            new Color(0, 255, 128),    // Mint
            new Color(255, 128, 128),  // Pink
            new Color(128, 255, 128),  // Light green
            new Color(128, 128, 255)   // Light blue
    };
    private final int totalBars = 16;
    // Volatile fields for thread safety
    private volatile long currentTick = 0;
    private volatile double currentBeat = 0.0;
    private volatile long currentBar = 0;
    private volatile boolean isPlaying = false;
    // Previous state to avoid unnecessary updates
    private long lastDisplayedTick = -1;
    private double lastDisplayedBeat = -1.0;
    private long lastDisplayedBar = -1;
    // UI state
    private JButton[][] currentButtons = null;
    private int lastPlayheadCol = -1;
    // Store original colors to fix trailing issue
    private Color[][] originalColors;
    // Store player activation data
    private boolean[][] playerActivations;
    private Player[] activePlayers;
    private int numPlayers = 0;
    // Refresh visualization on rule changes
    private boolean needsRefresh = true;
    // Scheduled executor for consistent frame rate
    private ScheduledExecutorService renderTimer;
    // Pre-calculated values
    private int ppq = 24;
    private int beatsPerBar = 4;
    private int ticksPerBar;
    private int totalTicks;
    private int columnsPerBeat;

    private static final Logger logger = LoggerFactory.getLogger(ScrollingSequencerVisualization.class);

    public ScrollingSequencerVisualization() {
        // Register for timing events
        try {
            TimingBus.getInstance().register(this);
            CommandBus.getInstance().register(this, new String[]{
                    Commands.TRANSPORT_START,
                    Commands.TRANSPORT_STOP,
                    Commands.METRONOME_START,
                    Commands.METRONOME_STOP,
                    Commands.TRANSPORT_STATE_CHANGED,
                    Commands.TIMING_UPDATE,
                    Commands.UPDATE_TEMPO,
                    Commands.TIMING_PARAMETERS_CHANGED,
                    Commands.SESSION_CHANGED,
                    Commands.PLAYER_ADDED,
                    Commands.PLAYER_DELETED,
                    Commands.RULE_ADDED,
                    Commands.RULE_EDITED,
                    Commands.RULE_DELETED,
                    Commands.SESSION_UPDATED,
                    Commands.PLAYER_UPDATE_EVENT
            });

            // Initialize with current state
            Session activeSession = SessionManager.getInstance().getActiveSession();
            if (activeSession != null) {
                isPlaying = activeSession.isRunning();
                currentTick = activeSession.getTick();
                currentBeat = activeSession.getBeat();
                currentBar = activeSession.getBar();

                // Pre-calculate timing values
                updateTimingParameters(activeSession);

                // Start the render timer if playing
                if (isPlaying) {
                    startRenderTimer();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize ScrollingSequencerVisualization", e);
        }
    }

    private void updateTimingParameters(Session session) {
        if (session != null) {
            ppq = session.getTicksPerBeat();
            beatsPerBar = session.getBeatsPerBar();
            ticksPerBar = ppq * beatsPerBar;
            totalTicks = ticksPerBar * totalBars;
        }
    }

    private void startRenderTimer() {
        if (renderTimer != null && !renderTimer.isShutdown()) {
            renderTimer.shutdown();
        }

        renderTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SequencerVisualizer-RenderThread");
            t.setDaemon(true); // Won't prevent app exit
            t.setPriority(Thread.MAX_PRIORITY); // Higher priority for smoother rendering
            return t;
        });

        // Schedule regular updates at the target frame rate
        renderTimer.scheduleAtFixedRate(
                this::updateDisplay,
                0,
                1000 / FRAMES_PER_SECOND,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopRenderTimer() {
        if (renderTimer != null && !renderTimer.isShutdown()) {
            renderTimer.shutdown();
            renderTimer = null;
        }
    }

    // This method is called by the render timer, not directly by timing events
    private void updateDisplay() {
        if (currentButtons == null || !isPlaying || originalColors == null) return;

        // Only update if something has changed
        if (currentTick == lastDisplayedTick &&
                currentBeat == lastDisplayedBeat &&
                currentBar == lastDisplayedBar) {
            return;
        }

        // Remember current state
        lastDisplayedTick = currentTick;
        lastDisplayedBeat = currentBeat;
        lastDisplayedBar = currentBar;

        // Execute UI updates on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                if (currentButtons == null) return;

                int cols = currentButtons[0].length;
                int rows = currentButtons.length;

                // Calculate new playhead position
                double currentPosition = (currentBar % totalBars) * ticksPerBar +
                        (currentBeat % beatsPerBar) * ppq +
                        currentTick;
                int playheadCol = (int) (currentPosition * cols) / totalTicks;

                // Only update if the playhead has moved
                if (playheadCol != lastPlayheadCol || lastPlayheadCol == -1) {
                    // Clear old playhead position by restoring original color
                    if (lastPlayheadCol >= 0 && lastPlayheadCol < cols) {
                        for (int row = 0; row < rows; row++) {
                            // Use saved original color instead of calculating
                            currentButtons[row][lastPlayheadCol].setBackground(
                                    originalColors[row][lastPlayheadCol]);
                        }
                    }

                    // Draw new playhead position
                    if (playheadCol >= 0 && playheadCol < cols) {
                        for (int row = 0; row < rows; row++) {
                            currentButtons[row][playheadCol].setBackground(POSITION_INDICATOR);
                        }
                    }

                    // Update text displays (only when needed)
                    if (rows > 2) {
                        currentButtons[0][0].setText(String.format("Beat: %d", currentBeat + 1));
                        currentButtons[1][0].setText(String.format("Bar: %d", currentBar + 1));
                        currentButtons[2][0].setText(String.format("Tick: %d", currentTick));
                    }

                    lastPlayheadCol = playheadCol;
                }
            } catch (Exception e) {
                // Prevent any exceptions from breaking the visualization
                // Log the exception for diagnostics
                java.util.logging.Logger.getLogger(ScrollingSequencerVisualization.class.getName()).log(java.util.logging.Level.SEVERE, "Visualization update failed", e);
            }
        });
    }

    @Override
    public void update(JButton[][] buttons) {
        // Store reference to buttons
        this.currentButtons = buttons;

        // If this is first call or refresh needed, initialize the grid
        if (lastPlayheadCol == -1 || needsRefresh) {
            initializeGrid(buttons);
            needsRefresh = false;
        }

        // Update display now
        updateDisplay();
    }

    private void initializeGrid(JButton[][] buttons) {
        SwingUtilities.invokeLater(() -> {
            if (buttons == null || buttons.length == 0) return;

            int cols = buttons[0].length;
            int rows = buttons.length;

            // Initialize the color storage array
            originalColors = new Color[rows][cols];

            // Clear all buttons first
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    buttons[row][col].setText("");
                    buttons[row][col].setBackground(BACKGROUND_COLOR);
                    originalColors[row][col] = BACKGROUND_COLOR;
                }
            }

            // Get latest timing parameters from active session
            Session activeSession = SessionManager.getInstance().getActiveSession();
            if (activeSession != null) {
                updateTimingParameters(activeSession);
            }

            // Calculate grid dimensions
            int beatsTotal = beatsPerBar * totalBars;
            columnsPerBeat = Math.max(1, cols / beatsTotal);

            // Draw beat and bar lines
            for (int beat = 0; beat < beatsTotal; beat++) {
                int x = beat * columnsPerBeat;

                // First determine if this is a bar line or beat line
                boolean isBarLine = beat % beatsPerBar == 0;
                Color lineColor = isBarLine ? BAR_MARKER_COLOR : BEAT_MARKER_COLOR;

                // Draw vertical lines for beats/bars
                for (int row = 0; row < rows; row++) {
                    if (x < cols) {
                        buttons[row][x].setBackground(lineColor);
                        originalColors[row][x] = lineColor; // Store the original color
                    }
                }
            }

            // Add labels row at the top
            if (rows > 1 && cols > 10) {
                buttons[0][0].setText("Players:");
            }

            // Get active players and evaluate when they will sound
            updateActivePlayers(rows, cols);

            // Reset playhead position tracker
            lastPlayheadCol = -1;
        });
    }

    private void updateActivePlayers(int rows, int cols) {
        Session session = SessionManager.getInstance().getActiveSession();
        if (session == null || session.getPlayers() == null || session.getPlayers().isEmpty()) {
            numPlayers = 0;
            activePlayers = null;
            return;
        }

        // Get active players (up to maximum rows - 1 for header)
        Set<Player> players = session.getPlayers();
        numPlayers = Math.min(players.size(), rows - 1);
        activePlayers = players.stream()
                .filter(p -> p.getRules() != null && !p.getRules().isEmpty())
                .limit(numPlayers)
                .toArray(Player[]::new);

        // Update player labels in first column
        for (int i = 0; i < numPlayers && i < rows - 1; i++) {
            if (currentButtons != null && currentButtons[i + 1] != null && currentButtons[i + 1][0] != null) {
                currentButtons[i + 1][0].setText(activePlayers[i].getName());
            }
        }

        // Pre-compute player activations for the visible portion
        evaluatePlayerActivations(cols);
    }

    private void evaluatePlayerActivations(int cols) {
        if (activePlayers == null || activePlayers.length == 0) return;

    // logger.debug("Evaluating activations for {} players across {} ticks", activePlayers.length, totalTicks);

        // Initialize activation map
        playerActivations = new boolean[activePlayers.length][cols];

        // For each player
        for (int playerIndex = 0; playerIndex < activePlayers.length; playerIndex++) {
            Player player = activePlayers[playerIndex];
            Set<Rule> rules = player.getRules();

            if (rules == null || rules.isEmpty()) {
                // logger.debug("Player {} has no rules, skipping", player.getName());
                continue;
            }

            // logger.debug("Evaluating player: {} with {} rules", player.getName(), rules.size());

            // Count how many hits we find
            // int hitCount = 0; // unused - removed

            // For each column in our grid
            for (int col = 0; col < cols; col++) {
                // Calculate musical position based on grid column
                int absoluteTick = (col * totalTicks) / cols;

                // Convert to musical units (1-based)
                int ticksPerBeat = ppq;
                int tickPosition = (absoluteTick % ticksPerBeat) + 1;
                int beatPosition = ((absoluteTick / ticksPerBeat) % beatsPerBar) + 1;
                int barPosition = ((absoluteTick / (ticksPerBeat * beatsPerBar)) % totalBars) + 1;

                // Use our new method
                boolean willPlay = player.shouldPlayAt(rules,
                        tickPosition,         // tick (1-based)
                        beatPosition,         // beat (1-based)
                        barPosition,          // bar (1-based)
                        1);                   // part (1-based, default to 1)

                if (willPlay) {

                    // Ensure we have space in our array
                    if (col < playerActivations[playerIndex].length) {
                        playerActivations[playerIndex][col] = true;

                        // Apply bright color to the grid
                        if (currentButtons != null &&
                                playerIndex + 1 < currentButtons.length &&
                                col < currentButtons[0].length) {

                            // Get player color
                            Color baseColor = PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
                            Color brightColor = new Color(
                                    Math.min(255, baseColor.getRed() + 80),
                                    Math.min(255, baseColor.getGreen() + 80),
                                    Math.min(255, baseColor.getBlue() + 80)
                            );

                            // Apply to button and store
                            currentButtons[playerIndex + 1][col].setBackground(brightColor);
                            originalColors[playerIndex + 1][col] = brightColor;

                            // Also label with player name in first column
                            if (col == 0) {
                                currentButtons[playerIndex + 1][col].setText(player.getName());
                            }
                        }
                    }
                }
            }

            // Debug: activations evaluated
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        switch (action.getCommand()) {
            // Transport state changes
            case Commands.TRANSPORT_START, Commands.METRONOME_START -> {
                isPlaying = true;
                startRenderTimer();
            }

            case Commands.TRANSPORT_STOP, Commands.METRONOME_STOP -> {
                isPlaying = false;
                resetPosition();
                stopRenderTimer();
            }

            case Commands.TRANSPORT_STATE_CHANGED -> {
                if (action.getData() instanceof Boolean playing) {
                    isPlaying = playing;
                    if (playing) {
                        startRenderTimer();
                    } else {
                        resetPosition();
                        stopRenderTimer();
                    }
                }
            }

            // Timing events
            case Commands.TIMING_UPDATE -> {
                if (action.getData() instanceof TimingUpdate timingUpdate) {
                    currentTick = timingUpdate.tick() != null ? timingUpdate.tick() : currentTick;
                    currentBeat = timingUpdate.beat() != null ? timingUpdate.beat() : currentBeat;
                    currentBar = timingUpdate.bar() != null ? timingUpdate.bar() : currentBar;
                }
            }

            // Listen for timing parameter changes
            case Commands.UPDATE_TEMPO, Commands.TIMING_PARAMETERS_CHANGED, Commands.SESSION_CHANGED -> {
                Session activeSession = SessionManager.getInstance().getActiveSession();
                if (activeSession != null) {
                    updateTimingParameters(activeSession);
                    // Re-initialize grid with new parameters
                    if (currentButtons != null) {
                        initializeGrid(currentButtons);
                    }
                }
            }

            // Add more cases to handle player/rule changes
            case Commands.PLAYER_ADDED, Commands.PLAYER_DELETED,
                 Commands.RULE_ADDED, Commands.RULE_EDITED, Commands.RULE_DELETED,
                 Commands.SESSION_UPDATED, Commands.PLAYER_UPDATE_EVENT -> {
                needsRefresh = true;
                if (currentButtons != null) {
                    initializeGrid(currentButtons);
                }
            }
        }
    }

    private void resetPosition() {
        currentTick = 0;
        currentBeat = 0;
        currentBar = 0;
        lastDisplayedTick = -1;
        lastDisplayedBeat = -1;
        lastDisplayedBar = -1;
        lastPlayheadCol = -1;

        // Re-initialize grid
        if (currentButtons != null) {
            initializeGrid(currentButtons);
        }
    }

    @Override
    public String getName() {
        return "16-Bar Sequencer";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MUSIC;
    }

    // Make sure to clean up resources
    public void cleanup() {
        stopRenderTimer();
        TimingBus.getInstance().unregister(this);
        CommandBus.getInstance().unregister(this);
        originalColors = null;
        currentButtons = null;
    }
}
