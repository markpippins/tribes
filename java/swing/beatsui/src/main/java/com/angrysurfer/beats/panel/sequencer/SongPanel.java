package com.angrysurfer.beats.panel.sequencer;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.PatternSlot;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SequencerService;
import com.angrysurfer.core.service.SequencerService;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.*;

public class SongPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(SongPanel.class);
    private static final int TRACK_HEIGHT = 40;
    private static final int BAR_WIDTH = 80;
    private static final int HEADER_HEIGHT = 25;
    private static final int TRACK_HEADER_WIDTH = 120;
    private final List<MelodicSequencer> melodicSequencers = new ArrayList<>();
    // Track pattern lists
    private final List<PatternSlot> drumPatternSlots = new ArrayList<>();
    private final Map<Integer, List<PatternSlot>> melodicPatternSlots = new HashMap<>();
    private final Point dragOffset = new Point();
    // Pattern sequencer internal class instance
    private final PatternSequencer patternSequencer;
    // Sequencers
    private DrumSequencer drumSequencer;
    // UI components
    private JPanel timelinePanel;
    private JPanel controlPanel;
    private JComboBox<String> trackCombo;
    private JComboBox<String> patternCombo;
    private JButton addPatternButton;
    private JButton deletePatternButton;
    private JSpinner lengthSpinner;
    // State variables
    private int songLength = 32; // Length in bars
    private int currentBar = 0;
    private PatternSlot selectedSlot = null;
    private PatternSlot draggingSlot = null;
    private boolean isDragging = false;
    // Add song mode toggle state
    private boolean songModeEnabled = false;
    private JToggleButton songModeToggle;

    public SongPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize with current bar = 0 (beginning of song)
        currentBar = 0;

        // Initialize busses - ensure this happens early
        CommandBus.getInstance().register(this, new String[]{
                Commands.TIMING_UPDATE,
                Commands.DRUM_PATTERN_SWITCHED,
                Commands.MELODIC_PATTERN_SWITCHED,
                Commands.TIMING_BAR
        });

        TimingBus.getInstance().register(this);

        // Initialize sequencer managers
        initializeSequencers();

        // Initialize pattern sequencer
        patternSequencer = new PatternSequencer();

        // Create UI
        createUI();

        // Add dummy data for testing
        addDummyPatternSlots();
    }

    private void initializeSequencers() {
        // Get drum sequencer from manager
        drumSequencer = SequencerService.getInstance().getDrumSequencer(0);

        // Get melodic sequencers
        for (int i = 0; i < SequencerService.getInstance().getSequencerCount(); i++) {
            MelodicSequencer sequencer = SequencerService.getInstance().getMelodicSequencer(i);
            if (sequencer != null) {
                melodicSequencers.add(sequencer);
                // Initialize empty pattern slot list for this sequencer
                melodicPatternSlots.put(sequencer.getId(), new ArrayList<>());
            }
        }
    }

    private void createUI() {
        // Create timeline panel with custom drawing
        timelinePanel = new TimelinePanel();
        JScrollPane scrollPane = new JScrollPane(timelinePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Create control panel
        controlPanel = createControlPanel();

        // Add panels to main layout
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0),
                BorderFactory.createTitledBorder("Song Controls")));

        // Control row 1 - Transport + Song Mode
        JPanel transportPanel = new JPanel();
        transportPanel.setLayout(new BoxLayout(transportPanel, BoxLayout.X_AXIS));

        JButton playButton = new JButton("▶ Play");
        playButton.addActionListener(e -> playSong());

        JButton stopButton = new JButton("⏹ Stop");
        stopButton.addActionListener(e -> stopSong());

        JButton rewindButton = new JButton("⏮ Rewind");
        rewindButton.addActionListener(e -> rewindSong());

        // Add song mode toggle with improved feedback and styling
        songModeToggle = new JToggleButton("Song Mode: OFF");
        songModeToggle.setSelected(false); // Match the initial songModeEnabled value
        songModeToggle.setBackground(new Color(200, 120, 120)); // Red for OFF
        songModeToggle.addActionListener(e -> {
            songModeEnabled = songModeToggle.isSelected();

            // Update the button text and color to provide visual feedback
            if (songModeEnabled) {
                songModeToggle.setText("Song Mode: ON");
                songModeToggle.setBackground(new Color(120, 200, 120));
                logger.info("Song mode enabled - patterns will auto-switch");
                patternSequencer.initializeSequencersForSongMode();
            } else {
                songModeToggle.setText("Song Mode: OFF");
                songModeToggle.setBackground(new Color(200, 120, 120));
                logger.info("Song mode disabled - patterns will NOT auto-switch");

                // Clear any queued next patterns when turning off song mode
                patternSequencer.clearAllQueuedPatterns();
            }

            // Force a repaint to update the timeline display
            timelinePanel.repaint();
        });

        transportPanel.add(rewindButton);
        transportPanel.add(Box.createHorizontalStrut(5));
        transportPanel.add(playButton);
        transportPanel.add(Box.createHorizontalStrut(5));
        transportPanel.add(stopButton);
        transportPanel.add(Box.createHorizontalStrut(20));
        transportPanel.add(songModeToggle);
        transportPanel.add(Box.createHorizontalGlue());

        // Control row 2 - Pattern Management
        JPanel patternPanel = new JPanel();
        patternPanel.setLayout(new BoxLayout(patternPanel, BoxLayout.X_AXIS));

        patternPanel.add(new JLabel("Track:"));
        patternPanel.add(Box.createHorizontalStrut(5));

        // Track selector
        String[] trackOptions = {"Drums", "Mono 1", "Mono 2", "Mono 3", "Mono 4"};
        trackCombo = new JComboBox<>(trackOptions);
        trackCombo.addActionListener(e -> updatePatternList());
        patternPanel.add(trackCombo);

        patternPanel.add(Box.createHorizontalStrut(10));
        patternPanel.add(new JLabel("Pattern:"));
        patternPanel.add(Box.createHorizontalStrut(5));

        // Pattern selector - will be populated when track is selected
        patternCombo = new JComboBox<>();
        patternPanel.add(patternCombo);

        patternPanel.add(Box.createHorizontalStrut(10));
        patternPanel.add(new JLabel("Length (bars):"));
        patternPanel.add(Box.createHorizontalStrut(5));

        // Length selector
        lengthSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
        patternPanel.add(lengthSpinner);

        patternPanel.add(Box.createHorizontalStrut(10));

        // Add pattern button
        addPatternButton = new JButton("Add");
        addPatternButton.addActionListener(e -> addPatternSlot());
        patternPanel.add(addPatternButton);

        patternPanel.add(Box.createHorizontalStrut(5));

        // Delete pattern button
        deletePatternButton = new JButton("Delete");
        deletePatternButton.addActionListener(e -> deleteSelectedPatternSlot());
        deletePatternButton.setEnabled(false); // Disabled until a slot is selected
        patternPanel.add(deletePatternButton);

        patternPanel.add(Box.createHorizontalGlue());

        // Song length controls
        JPanel lengthPanel = new JPanel();
        lengthPanel.setLayout(new BoxLayout(lengthPanel, BoxLayout.X_AXIS));

        lengthPanel.add(new JLabel("Song Length (bars):"));
        lengthPanel.add(Box.createHorizontalStrut(5));

        JSpinner songLengthSpinner = new JSpinner(new SpinnerNumberModel(songLength, 8, 128, 8));
        songLengthSpinner.addChangeListener(e -> {
            songLength = (int) songLengthSpinner.getValue();
            timelinePanel.revalidate();
            timelinePanel.repaint();
        });
        lengthPanel.add(songLengthSpinner);

        lengthPanel.add(Box.createHorizontalGlue());

        // Add all rows to control panel
        panel.add(transportPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(patternPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lengthPanel);

        // Initialize the pattern list
        updatePatternList();

        return panel;
    }

    private void updatePatternList() {
        patternCombo.removeAllItems();

        int selectedIndex = trackCombo.getSelectedIndex();
        if (selectedIndex == 0) {
            // Drum patterns
            List<Long> patternIds = SequencerService.getInstance().getAllDrumSequenceIds();
            for (Long id : patternIds) {
                patternCombo.addItem("Drum Pattern " + id);
            }
        } else if (selectedIndex > 0 && selectedIndex <= melodicSequencers.size()) {
            // Melodic patterns
            MelodicSequencer sequencer = melodicSequencers.get(selectedIndex - 1);
            List<Long> patternIds = SequencerService.getInstance().getAllMelodicSequenceIds(sequencer.getId());
            for (Long id : patternIds) {
                patternCombo.addItem("Pattern " + id);
            }
        }
    }

    private void addPatternSlot() {
        int selectedTrackIndex = trackCombo.getSelectedIndex();
        int patternIndex = patternCombo.getSelectedIndex();
        int length = (int) lengthSpinner.getValue();

        if (selectedTrackIndex >= 0 && patternIndex >= 0) {
            // Find insertion position (end of song or after selected slot)
            int position = 0;
            if (selectedSlot != null) {
                position = selectedSlot.getPosition() + selectedSlot.getLength();
            } else {
                // Find the end of existing patterns
                for (PatternSlot slot : drumPatternSlots) {
                    position = Math.max(position, slot.getPosition() + slot.getLength());
                }

                for (List<PatternSlot> slots : melodicPatternSlots.values()) {
                    for (PatternSlot slot : slots) {
                        position = Math.max(position, slot.getPosition() + slot.getLength());
                    }
                }
            }

            // Get pattern ID from the combo box text
            String patternText = (String) patternCombo.getSelectedItem();
            Long patternId = extractPatternId(patternText);

            if (patternId != null) {
                PatternSlot newSlot;

                if (selectedTrackIndex == 0) {
                    // Drum pattern
                    newSlot = new PatternSlot(patternId, position, length, "DRUM");
                    drumPatternSlots.add(newSlot);
                } else if (selectedTrackIndex <= melodicSequencers.size()) {
                    // Melodic pattern
                    MelodicSequencer sequencer = melodicSequencers.get(selectedTrackIndex - 1);
                    newSlot = new PatternSlot(patternId, position, length, "MELODIC", sequencer.getId());

                    List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
                    if (slots != null) {
                        slots.add(newSlot);
                    }
                }

                // Refresh display
                timelinePanel.repaint();
            }
        }
    }

    private Long extractPatternId(String patternText) {
        if (patternText == null) {
            return null;
        }

        try {
            // Extract number after "Pattern " or "Drum Pattern "
            String[] parts = patternText.split(" ");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            logger.error("Error extracting pattern ID from text: {}", patternText, e);
            return null;
        }
    }

    private void deleteSelectedPatternSlot() {
        if (selectedSlot == null) {
            return;
        }

        if ("DRUM".equals(selectedSlot.getType())) {
            drumPatternSlots.remove(selectedSlot);
        } else if ("MELODIC".equals(selectedSlot.getType()) && selectedSlot.getSequencerId() != null) {
            List<PatternSlot> slots = melodicPatternSlots.get(selectedSlot.getSequencerId());
            if (slots != null) {
                slots.remove(selectedSlot);
            }
        }

        selectedSlot = null;
        deletePatternButton.setEnabled(false);
        timelinePanel.repaint();
    }

    private void playSong() {
        // Start session playback
        if (!SessionManager.getInstance().getActiveSession().isRunning()) {
            // If song mode is enabled, initialize sequencers before starting playback
            if (songModeEnabled) {
                logger.info("Initializing sequencers for song mode");
                patternSequencer.initializeSequencersForSongMode();

                // Load initial patterns if we're at the beginning
                if (currentBar <= 1) {
                    logger.info("Loading initial patterns at bar: {}", currentBar);
                    patternSequencer.handleBarUpdate(currentBar);
                }
            } else {
                // Normal mode - ensure all sequencers are set to loop
                logger.info("Starting in normal mode (song mode disabled)");
                if (drumSequencer != null) {
                    drumSequencer.setLooping(true);
                }
                for (MelodicSequencer sequencer : melodicSequencers) {
                    sequencer.getSequenceData().setLooping(true);
                }
            }

            CommandBus.getInstance().publish(Commands.TRANSPORT_START, this);
        }
    }

    private void stopSong() {
        // Stop session playback
        if (SessionManager.getInstance().getActiveSession().isRunning()) {
            CommandBus.getInstance().publish(Commands.TRANSPORT_STOP, this);
        }
    }

    private void rewindSong() {
        // Rewind to beginning
        currentBar = 0;
        timelinePanel.repaint();
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (action.getData() instanceof TimingUpdate update) {
                    // Update current bar for display
                    int oldBar = currentBar;
                    currentBar = update.bar();

                    // Add debug output to confirm updates are coming through
                    logger.info("Bar changed: {} -> {} (sender: {})",
                            oldBar, currentBar, action.getSender().getClass().getSimpleName());

                    // Force repaint to ensure the grid updates
                    SwingUtilities.invokeLater(() -> {
                        timelinePanel.repaint();
                    });

                    // Delegate pattern switching to sequencer if song mode is enabled
                    if (songModeEnabled) {
                        patternSequencer.handleBarUpdate(currentBar);
                    }
                }
            }

            case Commands.DRUM_PATTERN_SWITCHED, Commands.MELODIC_PATTERN_SWITCHED -> {
                if (action.getData() instanceof PatternSwitchEvent event) {
                    String type = action.getCommand() == Commands.DRUM_PATTERN_SWITCHED ? "Drum" : "Melodic";
                    logger.info("{} pattern switched from {} to {}",
                            type, event.getPreviousPatternId(), event.getNewPatternId());
                }
            }
        }
    }

    // For testing only - adds sample pattern slots
    private void addDummyPatternSlots() {
        // Add some drum patterns
        drumPatternSlots.add(new PatternSlot(1L, 0, 4, "DRUM"));
        drumPatternSlots.add(new PatternSlot(2L, 4, 4, "DRUM"));
        drumPatternSlots.add(new PatternSlot(1L, 8, 8, "DRUM"));

        // Add some melodic patterns
        if (!melodicSequencers.isEmpty()) {
            MelodicSequencer seq1 = melodicSequencers.get(0);
            List<PatternSlot> seq1Slots = melodicPatternSlots.get(seq1.getId());
            seq1Slots.add(new PatternSlot(1L, 0, 8, "MELODIC", seq1.getId()));
            seq1Slots.add(new PatternSlot(2L, 8, 8, "MELODIC", seq1.getId()));

            if (melodicSequencers.size() > 1) {
                MelodicSequencer seq2 = melodicSequencers.get(1);
                List<PatternSlot> seq2Slots = melodicPatternSlots.get(seq2.getId());
                seq2Slots.add(new PatternSlot(1L, 4, 8, "MELODIC", seq2.getId()));
            }
        }
    }

    /**
     * Internal class to handle pattern sequencing logic
     */
    private class PatternSequencer {
        // Track which sequencers are currently active in the song
        private final Map<String, Boolean> activeSequencers = new HashMap<>();

        /**
         * Initialize all sequencers for song mode - disable looping
         */
        public void initializeSequencersForSongMode() {
            // Disable looping in all sequencers when starting song mode
            if (drumSequencer != null) {
                drumSequencer.setLooping(false);
                logger.debug("Initialized drum sequencer: looping disabled");
            }

            // Initialize melodic sequencers
            for (MelodicSequencer sequencer : melodicSequencers) {
                sequencer.getSequenceData().setLooping(false);
                logger.debug("Initialized melodic sequencer {}: looping disabled", sequencer.getId());
            }

            // Clear active tracking
            activeSequencers.clear();
        }

        /**
         * Handle a bar update from the timing system
         *
         * @param bar The current bar (1-based)
         */
        public void handleBarUpdate(int bar) {
            // The currentBar from timing updates is 1-based (starts at 1)
            // But our pattern positions are 0-based (start at 0)
            int zeroBasedCurrentBar = bar - 1;
            logger.debug("Handling bar update: bar={}, zeroBasedBar={}", bar, zeroBasedCurrentBar);

            // Check if we're entering or exiting pattern slots for each sequencer
            updateSequencerLoopingState(zeroBasedCurrentBar);

            // Look ahead to the next bar (still using 0-based index)
            int nextBar = zeroBasedCurrentBar + 1;
            logger.debug("Looking ahead to next bar: {}", nextBar + 1);

            // Check for drum pattern change
            PatternSlot nextDrumSlot = findSlotAtPosition(drumPatternSlots, nextBar);
            if (nextDrumSlot != null && drumSequencer != null) {
                drumSequencer.setNextPatternId(nextDrumSlot.getPatternId());
                logger.debug("Queuing drum pattern {} for bar {}",
                        nextDrumSlot.getPatternId(), nextBar + 1);
            }

            // Check for melodic pattern changes
            for (MelodicSequencer sequencer : melodicSequencers) {
                List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
                if (slots != null) {
                    PatternSlot nextSlot = findSlotAtPosition(slots, nextBar);
                    if (nextSlot != null) {
                        sequencer.setNextPatternId(nextSlot.getPatternId());
                        logger.debug("Queuing melodic pattern {} for sequencer {} at bar {}",
                                nextSlot.getPatternId(), sequencer.getId(), nextBar + 1);
                    }
                }
            }
        }

        /**
         * Update the looping state of all sequencers based on pattern slots
         */
        private void updateSequencerLoopingState(int currentBar) {
            // Handle drum sequencer
            updateSequencerLooping("drum", drumSequencer, drumPatternSlots, currentBar);

            // Handle melodic sequencers
            for (MelodicSequencer sequencer : melodicSequencers) {
                List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
                if (slots != null) {
                    updateSequencerLooping("melodic-" + sequencer.getId(), sequencer, slots, currentBar);
                }
            }
        }

        /**
         * Update looping state for a specific sequencer
         */
        private void updateSequencerLooping(String sequencerId, Object sequencer,
                                            List<PatternSlot> slots, int currentBar) {
            // Check if we're in a pattern slot
            PatternSlot currentSlot = findSlotAtPosition(slots, currentBar);
            boolean wasActive = activeSequencers.getOrDefault(sequencerId, false);

            if (currentSlot != null) {
                // We're in a pattern slot
                if (!wasActive) {
                    // Just entered a pattern slot - enable looping
                    setSequencerLooping(sequencer, true);
                    activeSequencers.put(sequencerId, true);
                    logger.debug("Enabling looping for {} at bar {} (pattern {})",
                            sequencerId, currentBar + 1, currentSlot.getPatternId());
                }

                // Check if we're at the last bar of this slot
                int slotEndPos = currentSlot.getPosition() + currentSlot.getLength() - 1;
                if (currentBar == slotEndPos) {
                    // Check if there's another slot immediately after this one
                    PatternSlot nextSlot = findSlotAtPosition(slots, currentBar + 1);
                    if (nextSlot == null) {
                        // No slot follows - disable looping at the end of this bar
                        setSequencerLooping(sequencer, false);
                        activeSequencers.put(sequencerId, false);
                        logger.debug("Disabling looping for {} after bar {} (end of pattern {})",
                                sequencerId, currentBar + 1, currentSlot.getPatternId());
                    }
                }
            } else if (wasActive) {
                // We just exited a pattern slot without entering a new one - disable looping
                setSequencerLooping(sequencer, false);
                activeSequencers.put(sequencerId, false);
                logger.debug("Disabling looping for {} at bar {} (exited pattern)",
                        sequencerId, currentBar + 1);
            }
        }

        /**
         * Helper method to set looping state based on sequencer type
         */
        private void setSequencerLooping(Object sequencer, boolean looping) {
            if (sequencer instanceof DrumSequencer) {
                ((DrumSequencer) sequencer).setLooping(looping);
            } else if (sequencer instanceof MelodicSequencer) {
                ((MelodicSequencer) sequencer).getSequenceData().setLooping(looping);
            }
        }

        /**
         * Find a pattern slot at a specific position
         */
        private PatternSlot findSlotAtPosition(List<PatternSlot> slots, int position) {
            for (PatternSlot slot : slots) {
                if (position >= slot.getPosition() && position < slot.getPosition() + slot.getLength()) {
                    return slot;
                }
            }
            return null;
        }

        /**
         * Clear all queued next patterns from sequencers and reset looping state
         */
        public void clearAllQueuedPatterns() {
            // Clear drum sequencer
            if (drumSequencer != null) {
                drumSequencer.setNextPatternId(null);
                drumSequencer.setLooping(true); // Reset to normal behavior when song mode is off
            }

            // Clear all melodic sequencers
            for (MelodicSequencer sequencer : melodicSequencers) {
                if (sequencer != null) {
                    sequencer.setNextPatternId(null);
                    sequencer.getSequenceData().setLooping(true); // Reset to normal behavior when song mode is off
                }
            }

            // Clear active tracking
            activeSequencers.clear();

            logger.info("Cleared all queued patterns and reset sequencer looping states");
        }
    }

    /**
     * Internal panel for custom timeline drawing
     */
    private class TimelinePanel extends JPanel {

        public TimelinePanel() {
            setLayout(null); // Absolute positioning for pattern blocks
            setBackground(Color.WHITE);// UIHelper.getBackgroundColor());

            // Track total components (1 drum track + melodic sequencers)
            int trackCount = 1 + melodicSequencers.size();

            // Set preferred size based on song length and track count
            int width = TRACK_HEADER_WIDTH + (songLength * BAR_WIDTH);
            int height = HEADER_HEIGHT + (trackCount * TRACK_HEIGHT);
            setPreferredSize(new Dimension(width, height));

            // Mouse listeners for drag & drop
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Check if clicking on a pattern slot
                    PatternSlot slot = findPatternSlotAt(e.getPoint());
                    if (slot != null) {
                        selectedSlot = slot;
                        draggingSlot = slot;
                        isDragging = true;
                        dragOffset.x = e.getX() - (TRACK_HEADER_WIDTH + (slot.getPosition() * BAR_WIDTH));
                        deletePatternButton.setEnabled(true);
                        repaint();
                    } else {
                        // Clicked on empty space
                        selectedSlot = null;
                        deletePatternButton.setEnabled(false);
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isDragging && draggingSlot != null) {
                        // Calculate new position based on BAR_WIDTH
                        int newX = e.getX() - dragOffset.x - TRACK_HEADER_WIDTH;
                        int newPosition = Math.max(0, newX / BAR_WIDTH);
                        draggingSlot.setPosition(newPosition);

                        // Ensure patterns don't overlap
                        if ("DRUM".equals(draggingSlot.getType())) {
                            resolveOverlaps(drumPatternSlots, draggingSlot);
                        } else if ("MELODIC".equals(draggingSlot.getType()) && draggingSlot.getSequencerId() != null) {
                            List<PatternSlot> slots = melodicPatternSlots.get(draggingSlot.getSequencerId());
                            if (slots != null) {
                                resolveOverlaps(slots, draggingSlot);
                            }
                        }
                    }

                    isDragging = false;
                    draggingSlot = null;
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDragging && draggingSlot != null) {
                        // Update selection rectangle during dragging
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw track headers background
            g2d.setColor(UIHelper.getAccentColor());
            g2d.fillRect(0, HEADER_HEIGHT, TRACK_HEADER_WIDTH, getHeight());

            // Draw timeline header background
            g2d.setColor(UIHelper.getAccentColor().darker());
            g2d.fillRect(0, 0, getWidth(), HEADER_HEIGHT);

            // Draw bar numbers
            g2d.setColor(UIHelper.getTextColor());
            g2d.setFont(new Font("Dialog", Font.PLAIN, 12));
            for (int i = 0; i < songLength; i++) {
                int x = TRACK_HEADER_WIDTH + (i * BAR_WIDTH) + (BAR_WIDTH / 2) - 10;
                g2d.drawString(Integer.toString(i + 1), x, 20);
            }

            // Draw bar dividers
            g2d.setColor(UIHelper.getTextColor().darker());
            for (int i = 0; i <= songLength; i++) {
                int x = TRACK_HEADER_WIDTH + (i * BAR_WIDTH);
                g2d.drawLine(x, 0, x, getHeight());
            }

            // Draw track names
            int trackY = HEADER_HEIGHT;
            g2d.setColor(UIHelper.getTextColor());

            // Drum track
            g2d.drawString("Drums", 10, trackY + (TRACK_HEIGHT / 2));
            trackY += TRACK_HEIGHT;

            // Melodic tracks
            for (int i = 0; i < melodicSequencers.size(); i++) {
                g2d.drawString("Mono " + (i + 1), 10, trackY + (TRACK_HEIGHT / 2));
                trackY += TRACK_HEIGHT;
            }

            // Draw track dividers
            g2d.setColor(UIHelper.getTextColor().darker());
            trackY = HEADER_HEIGHT;
            for (int i = 0; i <= 1 + melodicSequencers.size(); i++) {
                g2d.drawLine(0, trackY, getWidth(), trackY);
                trackY += TRACK_HEIGHT;
            }

            // Highlight current bar - FIXED VERSION
            if (currentBar >= 0) { // Remove session running check to always show position
                // g2d.setColor(new Color(255, 0, 0, 80)); // Semi-transparent red for better
                // visibility
                g2d.setColor(UIHelper.coolBlue);
                // Calculate bar position (account for 1-based vs 0-based indexing)
                int barX = TRACK_HEADER_WIDTH + ((currentBar > 0 ? currentBar - 1 : 0) * BAR_WIDTH);
                g2d.fillRect(barX, HEADER_HEIGHT, BAR_WIDTH, getHeight() - HEADER_HEIGHT);

                // Draw a more visible indicator at the top
                g2d.setColor(new Color(255, 0, 0));
                g2d.fillRect(barX, 0, BAR_WIDTH, HEADER_HEIGHT);
            }

            // Draw drum pattern slots
            trackY = HEADER_HEIGHT;
            drawPatternSlots(g2d, drumPatternSlots, trackY, UIHelper.warmMustard);

            // Draw melodic pattern slots
            trackY += TRACK_HEIGHT;
            for (MelodicSequencer sequencer : melodicSequencers) {
                List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
                if (slots != null) {
                    drawPatternSlots(g2d, slots, trackY, UIHelper.coolBlue);
                }
                trackY += TRACK_HEIGHT;
            }

            // Draw dragging slot if active
            if (isDragging && draggingSlot != null) {
                // Get track Y based on type and sequencer ID
                int dragTrackY = getDragTrackY(draggingSlot);

                // Calculate position based on mouse position
                Point mousePos = getMousePosition();
                if (mousePos != null) {
                    int dragX = mousePos.x - dragOffset.x;

                    // Draw semi-transparent dragging slot
                    Color slotColor = "DRUM".equals(draggingSlot.getType()) ? UIHelper.warmMustard : UIHelper.coolBlue;

                    g2d.setColor(new Color(slotColor.getRed(), slotColor.getGreen(),
                            slotColor.getBlue(), 128));

                    int slotWidth = draggingSlot.getLength() * BAR_WIDTH;
                    g2d.fillRect(dragX, dragTrackY + 5, slotWidth, TRACK_HEIGHT - 10);

                    // Draw border and pattern ID
                    g2d.setColor(Color.WHITE);
                    g2d.drawRect(dragX, dragTrackY + 5, slotWidth, TRACK_HEIGHT - 10);

                    String patternText = draggingSlot.getPatternId().toString();
                    g2d.drawString(patternText, dragX + 10, dragTrackY + (TRACK_HEIGHT / 2));
                }
            }
        }

        private void drawPatternSlots(Graphics2D g2d, List<PatternSlot> slots, int trackY, Color baseColor) {
            for (PatternSlot slot : slots) {
                int x = TRACK_HEADER_WIDTH + (slot.getPosition() * BAR_WIDTH);
                int width = slot.getLength() * BAR_WIDTH;

                // Draw pattern block background
                Color fillColor = slot == selectedSlot ? baseColor.brighter() : baseColor;
                g2d.setColor(fillColor);
                g2d.fillRect(x, trackY + 5, width, TRACK_HEIGHT - 10);

                // Draw border
                g2d.setColor(Color.WHITE);
                g2d.drawRect(x, trackY + 5, width, TRACK_HEIGHT - 10);

                // Draw pattern info
                g2d.setFont(new Font("Dialog", Font.BOLD, 12));
                g2d.drawString("Pattern " + slot.getPatternId(), x + 10, trackY + (TRACK_HEIGHT / 2));
            }
        }

        private PatternSlot findPatternSlotAt(Point point) {
            // Calculate which track the point is in
            int trackIndex = (point.y - HEADER_HEIGHT) / TRACK_HEIGHT;

            // Calculate bar position
            int barPos = (point.x - TRACK_HEADER_WIDTH) / BAR_WIDTH;

            // Check if point is within pattern slots
            if (trackIndex == 0) {
                // Drum track
                return findSlotContaining(drumPatternSlots, barPos);
            } else if (trackIndex > 0 && trackIndex <= melodicSequencers.size()) {
                // Melodic track
                MelodicSequencer sequencer = melodicSequencers.get(trackIndex - 1);
                List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
                return findSlotContaining(slots, barPos);
            }

            return null;
        }

        private PatternSlot findSlotContaining(List<PatternSlot> slots, int position) {
            if (slots == null)
                return null;

            for (PatternSlot slot : slots) {
                if (position >= slot.getPosition() && position < slot.getPosition() + slot.getLength()) {
                    return slot;
                }
            }
            return null;
        }

        private int getDragTrackY(PatternSlot slot) {
            if ("DRUM".equals(slot.getType())) {
                return HEADER_HEIGHT;
            } else if ("MELODIC".equals(slot.getType()) && slot.getSequencerId() != null) {
                // Find the index of the sequencer
                for (int i = 0; i < melodicSequencers.size(); i++) {
                    if (melodicSequencers.get(i).getId().equals(slot.getSequencerId())) {
                        return HEADER_HEIGHT + ((i + 1) * TRACK_HEIGHT);
                    }
                }
            }
            return HEADER_HEIGHT;
        }

        private void resolveOverlaps(List<PatternSlot> slots, PatternSlot movedSlot) {
            // Sort by position
            List<PatternSlot> sortedSlots = new ArrayList<>(slots);
            sortedSlots.sort(Comparator.comparing(PatternSlot::getPosition));

            // Check for and resolve overlaps
            for (int i = 0; i < sortedSlots.size() - 1; i++) {
                PatternSlot current = sortedSlots.get(i);
                PatternSlot next = sortedSlots.get(i + 1);

                // Skip if comparing the same slot
                if (current == next)
                    continue;

                // If current pattern overlaps with next pattern
                int currentEnd = current.getPosition() + current.getLength();
                if (currentEnd > next.getPosition()) {
                    // If moved slot is the second one, push it forward
                    if (movedSlot == next) {
                        next.setPosition(currentEnd);
                    }
                    // If moved slot is the first one, truncate it
                    else if (movedSlot == current) {
                        current.setLength(next.getPosition() - current.getPosition());
                    }
                }
            }
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);

            // Update preferred size when resized
            int requiredWidth = TRACK_HEADER_WIDTH + (songLength * BAR_WIDTH);
            int requiredHeight = HEADER_HEIGHT + ((1 + melodicSequencers.size()) * TRACK_HEIGHT);
            setPreferredSize(new Dimension(Math.max(width, requiredWidth), Math.max(height, requiredHeight)));
        }
    }
}
