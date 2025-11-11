package com.angrysurfer.beats.panel.player;

import com.angrysurfer.beats.panel.LivePanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.Comparison;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.SoundbankService;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class PlayerTimelinePanel extends LivePanel implements IBusListener {

    private static final Color GRID_BACKGROUND = Color.WHITE;
    private static final Color BAR_LINE_COLOR = new Color(100, 100, 120);
    private static final Color BEAT_LINE_COLOR = new Color(160, 160, 180);
    private static final Color ACTIVE_CELL_COLOR = new Color(41, 128, 185); // Cool blue color
    private static final Color COUNT_CELL_COLOR = Color.YELLOW; // Yellow for count rules
    private static final Color LABEL_PANEL_BACKGROUND = new Color(20, 20, 25); // Keep left panel dark
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 14);
    private static final int TICKS_PER_BEAT = 4; // Each beat shows 4 ticks

    // Add constants for the rule rows
    private static final int ROW_TICK = 0;
    private static final int ROW_TICK_COUNT = 1;
    private static final int ROW_BEAT = 2;
    private static final int ROW_BEAT_COUNT = 3;
    private static final int ROW_BAR = 4;
    private static final int ROW_BAR_COUNT = 5;
    private static final int ROW_PART = 6;
    private static final int ROW_PART_COUNT = 7;
    private static final int TOTAL_ROWS = 8;
    // Add these constants at the top of the class with the other constants
    private static final int RULE_TYPE_TICK = 0;
    private static final int RULE_TYPE_BEAT = 1;
    private static final int RULE_TYPE_BAR = 2;
    private static final int RULE_TYPE_PART = 3;
    // Add labels for the rows
    private JLabel ticksRowLabel;
    private JLabel beatsRowLabel;
    private JLabel barsRowLabel;
    private JLabel partsRowLabel;
    private Player player;
    private JPanel gridPanel;
    private JLabel nameLabel;
    private JPanel timeLabelsPanel;
    private Map<Point, JPanel> gridCells = new HashMap<>();
    private boolean[] activeBeatMap;
    // Replace cellSize with separate width and height
    private int cellWidth = 6; // Default cell width
    private int cellHeight = 15; // Default cell height (matches row height)
    private ComponentAdapter resizeListener;

    /**
     * Create an empty placeholder timeline that will be filled in when a player
     * is selected
     */
    public PlayerTimelinePanel() {
        super();
        setLayout(new BorderLayout());

        // Set a fixed size that won't change
        int fixedHeight = 200; // Reduced height
        setPreferredSize(new Dimension(800, fixedHeight));
        setMinimumSize(new Dimension(200, fixedHeight));
        setMaximumSize(new Dimension(Short.MAX_VALUE, fixedHeight));

        // Create the empty grid with initial placeholders
        initEmptyComponents();

        // Register for specific events only
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_SELECTION_EVENT,
                Commands.PLAYER_UPDATE_EVENT,
                Commands.PLAYER_RULE_UPDATE_EVENT,
                Commands.NEW_VALUE_NOTE,
                Commands.PRESET_UP,
                Commands.PRESET_DOWN,
                Commands.PLAYER_ROW_REFRESH,
                Commands.SESSION_CHANGED
        });
    }

    @Override
    public void handlePlayerActivated() {
        this.player = player;

        if (player == null) {
            // Show empty placeholder but still draw grid
            nameLabel.setText("Select a player to view timeline");

            // Clear any existing cells but still show the grid structure
            clearGrid();

            // Draw empty grid with default values
            drawEmptyTimelineGrid();
        } else {
            // Show timeline with fixed row heights
            updateTimelineWithFixedRowHeights();
        }

        // Repaint after changes
        repaint();
    }

    @Override
    public void handlePlayerUpdated() {
        this.player = player;

        if (player == null) {
            // Show empty placeholder but still draw grid
            nameLabel.setText("Select a player to view timeline");

            // Clear any existing cells but still show the grid structure
            clearGrid();

            // Draw empty grid with default values
            drawEmptyTimelineGrid();
        } else {
            // Show timeline with fixed row heights
            updateTimelineWithFixedRowHeights();
        }

        // Repaint after changes
        repaint();
    }

    /**
     * Draw an empty grid structure when no player is selected
     */
    private void drawEmptyTimelineGrid() {
        // Clear existing content
        gridCells.clear();

        // Add row labels with fixed height
        int rowHeight = cellHeight;
        addRowLabelsWithFixedHeight(rowHeight);

        // Set grid dimensions - use default values
        int defaultTicks = 16 * 6; // Assume 16 beats at 6 ticks per beat
        int gridWidth = defaultTicks * cellWidth + 85;
        int gridHeight = rowHeight * TOTAL_ROWS;
        gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));

        addEmptyTimeLabels(SessionManager.getInstance().getActiveSession().getBeatsPerBar(), SessionManager.getInstance().getActiveSession().getBars());

        // Revalidate to apply changes
        gridPanel.revalidate();
        timeLabelsPanel.revalidate();
    }

    /**
     * Add time labels with default values when no player is selected
     */
    private void addEmptyTimeLabels(int beatsPerBar, int bars) {
        timeLabelsPanel.removeAll();

        int ticksPerBeat = 6; // Default value
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;
        int labelWidth = 40;
        int totalWidth = labelWidth + totalTicks * cellWidth;

        // Set size for time labels panel
        timeLabelsPanel.setPreferredSize(new Dimension(totalWidth, 20));
        timeLabelsPanel.setSize(totalWidth, 30);

        // Calculate vertical centering position
        int panelHeight = 20;
        int fontSize = 12;
        int yCenter = (panelHeight - fontSize) / 2;

        // Add bar numbers
        for (int bar = 0; bar < bars; bar++) {
            JLabel barLabel = new JLabel(String.valueOf(bar + 1));
            barLabel.setForeground(Color.WHITE);
            barLabel.setFont(new Font("Arial", Font.BOLD, 12));

            int barWidth = beatsPerBar * ticksPerBeat * cellWidth;
            int x = labelWidth + bar * barWidth + barWidth / 2 - 5;
            barLabel.setBounds(x, yCenter, 20, fontSize);

            timeLabelsPanel.add(barLabel);
        }

        // Add beat numbers
        for (int bar = 0; bar < bars; bar++) {
            for (int beat = 0; beat < beatsPerBar; beat++) {
                JLabel beatLabel = new JLabel(String.valueOf(beat + 1));
                beatLabel.setForeground(Color.LIGHT_GRAY);
                beatLabel.setFont(new Font("Arial", Font.PLAIN, 10));

                int x = labelWidth + (bar * beatsPerBar * ticksPerBeat + beat * ticksPerBeat) * cellWidth
                        + (ticksPerBeat * cellWidth / 2) - 3;
                beatLabel.setBounds(x, yCenter + 1, 10, fontSize - 2);

                timeLabelsPanel.add(beatLabel);
            }
        }

        // Ensure panel is visible
        timeLabelsPanel.setVisible(true);
    }

    /**
     * Override paintComponent to draw grid lines even when no player is
     * selected
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw grid lines even if no player is selected
        if (player == null) {
            // Draw empty grid lines with default values
            drawEmptyGridLines(g);
        }
    }

    /**
     * Draw grid lines with default values when no player is selected
     */
    private void drawEmptyGridLines(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Use default values
        int beatsPerBar = 4;
        int bars = 4;
        int ticksPerBeat = 6;
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Account for label panel width
        int labelWidth = 40;
        int rowHeight = cellHeight;

        // Draw horizontal row dividers
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i < TOTAL_ROWS; i++) {
            int y = i * rowHeight;
            g2d.drawLine(labelWidth, y, labelWidth + totalTicks * cellWidth, y);
        }

        // Draw vertical beat lines
        for (int beat = 0; beat <= totalBeats; beat++) {
            int x = labelWidth + beat * ticksPerBeat * cellWidth;

            if (beat % beatsPerBar == 0) {
                // Draw bar lines with thicker stroke
                g2d.setColor(BAR_LINE_COLOR);
                g2d.setStroke(new BasicStroke(2));
            } else {
                // Draw beat lines with thinner stroke
                g2d.setColor(BEAT_LINE_COLOR);
                g2d.setStroke(new BasicStroke(1));
            }

            g2d.drawLine(x, 0, x, rowHeight * TOTAL_ROWS);
        }

        // Draw vertical tick lines (thinner)
        g2d.setColor(new Color(220, 220, 220)); // Very light gray
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1, 2}, 0));
        for (int tick = 0; tick <= totalTicks; tick++) {
            // Skip lines that are already drawn as beat or bar lines
            if (tick % ticksPerBeat != 0) {
                int x = labelWidth + tick * cellWidth;
                g2d.drawLine(x, 0, x, rowHeight * TOTAL_ROWS);
            }
        }
    }

    /**
     * Initialize empty components with placeholders
     */
    private void initEmptyComponents() {

        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Create header with player name - keep minimal
        nameLabel = new JLabel("Select a player to view timeline");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        // Create zoom control panel
        JPanel zoomControlPanel = new JPanel(new BorderLayout(5, 0));
        zoomControlPanel.setOpaque(false);

        // Create zoom buttons with proper styling
        JButton zoomOutButton = new JButton("-");
        zoomOutButton.setFont(new Font("Arial", Font.BOLD, 14));
        zoomOutButton.setFocusPainted(false);
        // zoomOutButton.setMargin(new Insets(0, 2, 0, 2));

        JButton zoomInButton = new JButton("+");
        zoomInButton.setFont(new Font("Arial", Font.BOLD, 14));
        zoomInButton.setFocusPainted(false);
        // zoomInButton.setMargin(new Insets(0, 2, 0, 2));

        // Add action listeners for zoom buttons
        zoomOutButton.addActionListener(e -> {
            // Decrease cell width but keep minimum of 4px
            if (cellWidth > 4) {
                cellWidth--;
                updateGridAfterZoom();
            }
        });

        zoomInButton.addActionListener(e -> {
            // Increase cell width (no practical upper limit needed)
            cellWidth++;
            updateGridAfterZoom();
        });

        // Add buttons to zoom panel
        zoomControlPanel.add(zoomOutButton, BorderLayout.WEST);
        zoomControlPanel.add(zoomInButton, BorderLayout.EAST);
        zoomControlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        // Use BorderLayout for infoPanel to place buttons on the right
        JPanel infoPanel = new JPanel(new BorderLayout());
        // infoPanel.setBackground(UIHelper.coolBlue);
        infoPanel.add(nameLabel, BorderLayout.CENTER);
        infoPanel.add(zoomControlPanel, BorderLayout.EAST);

        infoPanel.setMinimumSize(new Dimension(800, 30));
        infoPanel.setPreferredSize(new Dimension(800, 30));

        add(infoPanel, BorderLayout.NORTH);

        // Create main grid panel with fixed cell size
        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (player != null) {
                    drawGridLines(g);
                }
            }
        };
        gridPanel.setLayout(null);
        // gridPanel.setBackground(GRID_BACKGROUND);

        // Create a content panel with proper size constraints
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(GRID_BACKGROUND);

        // Calculate initial grid height based on row height
        // IMPORTANT: Use exact row count to eliminate extra space
        int initialGridHeight = cellHeight * TOTAL_ROWS; // This should be exactly 8 rows (120px)
        gridPanel.setPreferredSize(new Dimension(800, initialGridHeight));

        // Time labels panel with minimal height
        timeLabelsPanel = new JPanel();
        timeLabelsPanel.setLayout(null);
        // timeLabelsPanel.setBackground(UIHelper.coolBlue);
        timeLabelsPanel.setPreferredSize(new Dimension(800, 20)); // Keep minimal
        timeLabelsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE));

        // Add components to content panel with no gaps
        contentPanel.add(gridPanel, BorderLayout.CENTER);
        contentPanel.add(timeLabelsPanel, BorderLayout.SOUTH);

        // No gaps or extra spaces in the scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Update the grid after zoom level changes
     */
    private void updateGridAfterZoom() {
        SwingUtilities.invokeLater(() -> {
            if (player != null) {
                // Refresh with player data
                updateTimelineWithFixedRowHeights();
            } else {
                // Refresh empty grid
                drawEmptyTimelineGrid();
            }

            // Make sure scrollbars update
            revalidate();
            repaint();

            // Ensure current position stays visible
            scrollToCurrentPosition();
        });
    }

    // Override this method to enforce the fixed size
    @Override
    public Dimension getPreferredSize() {
        // Always return our fixed size to prevent layout changes
        return new Dimension(800, 200); // Reduced from 230px to 200px
    }

    /**
     * Update the timeline with fixed row heights for consistency
     */
    private void updateTimelineWithFixedRowHeights() {
        if (player == null || player.getSession() == null) {
            return;
        }

        // Clear existing content
        clearGrid();

        // Update player name
        updateNameLabel();

        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Use reduced row height for consistency
        int rowHeight = cellHeight;

        // Set grid size
        int gridWidth = totalTicks * cellWidth + 85;
        int gridHeight = rowHeight * TOTAL_ROWS;
        gridPanel.setPreferredSize(new Dimension(gridWidth, gridHeight));

        // Add row labels with fixed height
        addRowLabelsWithFixedHeight(rowHeight);

        // Calculate active rules
        boolean[][] activeRules = calculateActiveRules(player, session);

        // Add rule cells with fixed height
        for (int tick = 0; tick < totalTicks; tick++) {
            // Tick rules
            addRuleCell(tick, ROW_TICK, activeRules[ROW_TICK][tick], rowHeight);
            addRuleCell(tick, ROW_TICK_COUNT, activeRules[ROW_TICK_COUNT][tick], rowHeight);

            // Beat rules at beat boundaries
            if (tick % ticksPerBeat == 0) {
                int beatIndex = tick / ticksPerBeat;
                addRuleCell(tick, ROW_BEAT, activeRules[ROW_BEAT][beatIndex], rowHeight);
                addRuleCell(tick, ROW_BEAT_COUNT, activeRules[ROW_BEAT_COUNT][beatIndex], rowHeight);
            }

            // Bar rules at bar boundaries
            if (tick % (beatsPerBar * ticksPerBeat) == 0) {
                int barIndex = tick / (beatsPerBar * ticksPerBeat);
                addRuleCell(tick, ROW_BAR, activeRules[ROW_BAR][barIndex], rowHeight);
                addRuleCell(tick, ROW_BAR_COUNT, activeRules[ROW_BAR_COUNT][barIndex], rowHeight);
            }

            // Part rules at beginning
            if (tick == 0) {
                addRuleCell(tick, ROW_PART, activeRules[ROW_PART][0], rowHeight);
                addRuleCell(tick, ROW_PART_COUNT, activeRules[ROW_PART_COUNT][0], rowHeight);
            }
        }

        // Update time labels
        updateTimeLabels(beatsPerBar, bars);
    }

    /**
     * Add row labels with fixed height
     */
    private void addRowLabelsWithFixedHeight(int rowHeight) {
        // Create label panel on the left
        JPanel labelPanel = new JPanel(null);
        // labelPanel.setBackground(LABEL_PANEL_BACKGROUND);
        labelPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BAR_LINE_COLOR));
        labelPanel.setPreferredSize(new Dimension(80, rowHeight * TOTAL_ROWS));

        // Add to grid
        gridPanel.add(labelPanel);
        labelPanel.setBounds(0, 0, 40, rowHeight * TOTAL_ROWS);

        // Create labels
        String[] labelTexts = {"Tick", "Ticks", "Beat", "Beats", "Bar", "Bars", "Part", "Parts"};
        for (int i = 0; i < TOTAL_ROWS; i++) {
            JLabel label = createRowLabel(labelTexts[i]);
            label.setBounds(0, i * rowHeight, 80, rowHeight);
            labelPanel.add(label);
        }
    }

    private JLabel createRowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        // Use a smaller font for the reduced row height
        label.setFont(new Font("Arial", Font.BOLD, 10));
        label.setHorizontalAlignment(JLabel.LEFT);
        return label;
    }

    /**
     * Calculate which ticks/beats/bars have active rules Returns a 2D array:
     * [row type][index] -> active?
     */
    private boolean[][] calculateActiveRules(Player player, Session session) {
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Create arrays for ALL row types (standard AND count)
        boolean[][] results = new boolean[TOTAL_ROWS][];
        results[ROW_TICK] = new boolean[totalTicks];
        results[ROW_TICK_COUNT] = new boolean[totalTicks];
        results[ROW_BEAT] = new boolean[totalBeats];
        results[ROW_BEAT_COUNT] = new boolean[totalBeats];
        results[ROW_BAR] = new boolean[bars];
        results[ROW_BAR_COUNT] = new boolean[bars];
        results[ROW_PART] = new boolean[1];
        results[ROW_PART_COUNT] = new boolean[1];

        // Get all player rules
        Set<Rule> allRules = player.getRules();

        // Process standard position rules
        for (int tickIndex = 0; tickIndex < totalTicks; tickIndex++) {
            // Calculate position within the session
            int bar = tickIndex / (beatsPerBar * ticksPerBeat);
            int beatInBar = (tickIndex % (beatsPerBar * ticksPerBeat)) / ticksPerBeat;
            int tickInBeat = tickIndex % ticksPerBeat;
            int beatIndex = bar * beatsPerBar + beatInBar;

            // Convert to session values (1-based)
            long sessionTick = tickInBeat + 1;
            double sessionBeat = beatInBar + 1;
            long sessionBar = bar + 1;

            // Check for tick/beat/bar/part rules
            for (Rule rule : allRules) {
                if (rule.getPart() != 0 && rule.getPart() != 1) {
                    continue;
                }

                // Use integer constants from Comparison class instead of strings
                switch (rule.getComparison()) {
                    // Standard position rules
                    case Comparison.TICK: // Use constant instead of "TICK"
                        if (rule.getValue() == sessionTick) {
                            results[ROW_TICK][tickIndex] = true;
                        }
                        break;
                    case Comparison.BEAT: // Use constant instead of "BEAT"
                        if (rule.getValue() == sessionBeat) {
                            results[ROW_BEAT][beatIndex] = true;
                        }
                        break;
                    case Comparison.BAR: // Use constant instead of "BAR"
                        if (rule.getValue() == sessionBar) {
                            results[ROW_BAR][bar] = true;
                        }
                        break;
                    case Comparison.PART: // Use constant instead of "PART"
                        results[ROW_PART][0] = true;
                        break;
                }
            }
        }

        // Process COUNT rules separately
        for (Rule rule : allRules) {
            if (rule.getPart() != 0 && rule.getPart() != 1) {
                continue;
            }

            // Use integer constants instead of strings
            switch (rule.getComparison()) {
                case Comparison.TICK_COUNT: // Use constant instead of "TICK_COUNT"
                    // For tick count rules, mark where they would match
                    for (int i = 0; i < totalTicks; i++) {
                        int tickValue = i + 1; // 1-based counting
                        if (rule.matches(tickValue)) {
                            results[ROW_TICK_COUNT][i] = true;
                        }
                    }
                    break;

                case Comparison.BEAT_COUNT: // Use constant instead of "BEAT_COUNT"
                    // For beat count rules
                    for (int i = 0; i < totalBeats; i++) {
                        int beatValue = i + 1; // 1-based counting
                        if (rule.matches(beatValue)) {
                            results[ROW_BEAT_COUNT][i] = true;
                        }
                    }
                    break;

                case Comparison.BAR_COUNT: // Use constant instead of "BAR_COUNT"
                    // For bar count rules
                    for (int i = 0; i < bars; i++) {
                        int barValue = i + 1; // 1-based counting
                        if (rule.matches(barValue)) {
                            results[ROW_BAR_COUNT][i] = true;
                        }
                    }
                    break;

                case Comparison.PART_COUNT: // Use constant instead of "PART_COUNT"
                    // Part count rules generally apply to the whole part
                    results[ROW_PART_COUNT][0] = true;
                    break;
            }
        }

        return results;
    }

    /**
     * Add a cell to represent a rule in the grid
     */
    private void addRuleCell(int tickIndex, int rowType, boolean isActive, int rowHeight) {
        if (!isActive) {
            return; // Only add cells for active rules
        }
        // Account for label panel width
        int labelWidth = 40;

        // UPDATED: Position cell exactly at the row with no margin
        int x = labelWidth + tickIndex * cellWidth + 1; // Add label width with 1px offset
        int y = rowType * rowHeight; // No margin, align to row top

        JPanel cell = new JPanel();

        // Use yellow for count rules, blue for standard rules
        boolean isCountRule = (rowType == ROW_TICK_COUNT || rowType == ROW_BEAT_COUNT || rowType == ROW_BAR_COUNT
                || rowType == ROW_PART_COUNT);
        cell.setBackground(isActive ? (isCountRule ? COUNT_CELL_COLOR : ACTIVE_CELL_COLOR) : GRID_BACKGROUND);

        // Adjust width based on type
        int width;
        if (rowType == ROW_TICK) {
            width = cellWidth - 2;
        } else if (rowType == ROW_BEAT) {
            int ticksPerBeat = player.getSession().getTicksPerBeat();
            width = ticksPerBeat * cellWidth - 2;
        } else if (rowType == ROW_BAR) {
            Session session = player.getSession();
            int ticksPerBeat = session.getTicksPerBeat();
            width = session.getBeatsPerBar() * ticksPerBeat * cellWidth - 2;
        } else { // PARTS
            width = player.getSession().getBars() * player.getSession().getBeatsPerBar()
                    * player.getSession().getTicksPerBeat() * cellWidth - 2;
        }

        // UPDATED: Set height to full row height
        cell.setBounds(x, y, width, rowHeight);
        cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        gridPanel.add(cell);
        gridCells.put(new Point(tickIndex, rowType), cell);
    }

    /**
     * Update time labels at the bottom with bars and beats
     */
    private void updateTimeLabels(int beatsPerBar, int bars) {
        timeLabelsPanel.removeAll();

        // Account for label panel width
        int labelWidth = 40;

        // Add bar numbers - show 1-based values to match Session's 1-based counting
        for (int bar = 0; bar < bars; bar++) {
            JLabel barLabel = new JLabel(String.valueOf(bar + 1)); // Display 1-based bar numbers
            barLabel.setForeground(Color.WHITE);
            barLabel.setFont(new Font("Arial", Font.BOLD, 12));

            int barWidth = beatsPerBar * player.getSession().getTicksPerBeat() * cellWidth;
            // Add labelWidth to x position to account for left panel
            int x = labelWidth + bar * barWidth + barWidth / 2 - 5; // Center in bar
            barLabel.setBounds(x, 0, 20, 20);

            timeLabelsPanel.add(barLabel);
        }

        // Add beat numbers for each bar - show 1-based values to match Session
        for (int bar = 0; bar < bars; bar++) {
            for (int beat = 0; beat < beatsPerBar; beat++) {
                JLabel beatLabel = new JLabel(String.valueOf(beat + 1)); // Display 1-based beat numbers
                beatLabel.setForeground(Color.LIGHT_GRAY);
                beatLabel.setFont(new Font("Arial", Font.PLAIN, 10));

                // Add labelWidth to x position to account for left panel
                int x = labelWidth
                        + (bar * beatsPerBar * player.getSession().getTicksPerBeat()
                        + beat * player.getSession().getTicksPerBeat()) * cellWidth
                        + (player.getSession().getTicksPerBeat() * cellWidth / 2) - 3; // Center in beat
                beatLabel.setBounds(x, 10, 10, 10);

                timeLabelsPanel.add(beatLabel);
            }
        }
    }

    /**
     * Clear the grid display
     */
    private void clearGrid() {
        gridPanel.removeAll();
        gridCells.clear();
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            switch (action.getCommand()) {
                case Commands.PLAYER_SELECTION_EVENT -> {
                    if (action.getData() instanceof Player p) {
                        player = p;
                        updateTimelineWithFixedRowHeights();
                    }
                }
                // case Commands.PLAYER_UNSELECTED -> {
                //     player = null;
                //     clearGrid();
                //     nameLabel.setText("Select a player to view timeline");
                //     // Add this line to redraw the empty grid structure when player is unselected
                //     drawEmptyTimelineGrid();
                // }
                case Commands.PLAYER_UPDATE_EVENT -> {
                    if (player != null && action.getData() instanceof PlayerUpdateEvent event &&
                            event.getPlayer().getId().equals(player.getId())) {
                        player = event.getPlayer();
                        updateTimelineWithFixedRowHeights();
                    }
                }
                // Add handler for note changes
                case Commands.NEW_VALUE_NOTE, Commands.PRESET_UP, Commands.PRESET_DOWN, Commands.PLAYER_ROW_REFRESH ->
                        updateNameLabel();

                case Commands.SESSION_CHANGED -> {
                    if (player != null) {
                        updateTimelineWithFixedRowHeights();
                    } else {
                        // Also draw empty grid if no player is selected but session changes
                        drawEmptyTimelineGrid();
                    }
                }
            }
        });
    }

    /**
     * Updates just the player name label without redrawing the entire grid
     */
    private void updateNameLabel() {
        if (player == null) {
            nameLabel.setText("Select a player to view timeline");
            return;
        }

        StringBuilder playerInfo = new StringBuilder();

        // Start with player name
        playerInfo.append(player.getName());

        // Add instrument information if available
        if (player.getInstrument() != null) {
            playerInfo.append(" - ").append(player.getInstrument().getName());

            // Add device name if it's different from instrument name
            String deviceName = player.getInstrument().getDeviceName();
            if (deviceName != null && !deviceName.isEmpty() && !deviceName.equals(player.getInstrument().getName())) {
                playerInfo.append(" (").append(deviceName).append(")");
            }

            // Add soundbank name if available
            String soundbankName = player.getInstrument().getSoundbankName();
            if (soundbankName != null && !soundbankName.isEmpty()) {
                playerInfo.append(" [").append(soundbankName).append("]");
            }

            // Get preset name if available
            if (player.getPreset() != null) {
                Long instrumentId = player.getInstrument().getId();
                Long presetNumber = player.getPreset().longValue();

                // For channel 9 (MIDI channel 10), show drum name instead of preset
                if (Objects.equals(player.getChannel(), SequencerConstants.MIDI_DRUM_CHANNEL)) {
                    // Get drum name for the note
                    String drumName = MidiService.getInstance().getDrumName(player.getRootNote());
                    playerInfo.append(" - ").append(drumName);
                } else if (soundbankName != null && !soundbankName.isEmpty()) {
                    // For instruments with loaded soundbanks
                    Integer bankIndex = player.getInstrument().getBankIndex();
                    if (bankIndex == null)
                        bankIndex = 0;

                    // Try to get preset name from soundbank
                    try {
                        String presetName = SoundbankService.getInstance()
                                .getPresetNames(soundbankName, bankIndex)
                                .stream()
                                .skip(presetNumber)
                                .findFirst()
                                .orElse("Preset " + presetNumber);

                        playerInfo.append(" - ").append(presetName);
                    } catch (Exception e) {
                        // Fallback to just showing preset number
                        playerInfo.append(" - Preset ").append(presetNumber);
                    }
                } else {
                    // For standard internal instruments
                    String presetName = SoundbankService.getInstance().getPresetName(instrumentId, presetNumber);
                    if (presetName != null && !presetName.isEmpty()) {
                        playerInfo.append(" - ").append(presetName);
                    }
                }
            }
        }

        // Update the name label with all the information
        nameLabel.setText(playerInfo.toString());

        // Request focus to ensure the UI updates
        nameLabel.repaint();
    }

    public void scrollToCurrentPosition() {
        // Scroll to make the current position visible
        // This is particularly useful after resizing
        Rectangle visibleRect = new Rectangle(0, 0, 10, 10);
        scrollRectToVisible(visibleRect);
    }

    /**
     * Draw the grid lines (vertical for beats/bars, horizontal for rows)
     */
    private void drawGridLines(Graphics g) {
        if (player == null || player.getSession() == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Session session = player.getSession();
        int beatsPerBar = session.getBeatsPerBar();
        int bars = session.getBars();
        int ticksPerBeat = session.getTicksPerBeat();
        int totalBeats = beatsPerBar * bars;
        int totalTicks = totalBeats * ticksPerBeat;

        // Account for label panel width
        int labelWidth = 40;
        int rowHeight = cellHeight; // Match the reduced row height we use elsewhere

        // Draw horizontal row dividers
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i < TOTAL_ROWS; i++) {
            int y = i * rowHeight;
            g2d.drawLine(labelWidth, y, labelWidth + totalTicks * cellWidth, y);
        }

        // Draw vertical beat lines
        for (int beat = 0; beat <= totalBeats; beat++) {
            int x = labelWidth + beat * ticksPerBeat * cellWidth;

            if (beat % beatsPerBar == 0) {
                // Draw bar lines with thicker stroke
                g2d.setColor(BAR_LINE_COLOR);
                g2d.setStroke(new BasicStroke(2));
            } else {
                // Draw beat lines with thinner stroke
                g2d.setColor(BEAT_LINE_COLOR);
                g2d.setStroke(new BasicStroke(1));
            }

            g2d.drawLine(x, 0, x, rowHeight * TOTAL_ROWS);
        }

        // Draw vertical tick lines (thinner)
        g2d.setColor(new Color(220, 220, 220)); // Very light gray
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1, 2}, 0));
        for (int tick = 0; tick <= totalTicks; tick++) {
            // Skip lines that are already drawn as beat or bar lines
            if (tick % ticksPerBeat != 0) {
                int x = labelWidth + tick * cellWidth;
                g2d.drawLine(x, 0, x, rowHeight * TOTAL_ROWS);
            }
        }
    }
}
