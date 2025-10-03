package com.angrysurfer.beats.panel.sequencer.mono;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.OffsetSequencerPanel2;
import com.angrysurfer.beats.panel.sequencer.TiltSequencerPanel;
import com.angrysurfer.beats.presenter.MelodicSequencerPresenter;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.event.PlayerRefreshEvent;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.MelodicSequencerManager;
import com.angrysurfer.core.service.SoundbankManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencerPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerPanel.class);

    private JToggleButton loopToggleButton;

    // CORE SEQUENCER - manages all sequencing logic
    private MelodicSequencer sequencer;

    // UI state variables - keep these in the panel

    // Labels and UI components
    private JLabel octaveLabel;
    private JComboBox<String> rangeCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton latchToggleButton;

    private MelodicSequenceNavigationPanel navigationPanel;

    private MelodicSequencerSwingPanel swingPanel;

    private MelodicSequenceParametersPanel sequenceParamsPanel;

    private TiltSequencerPanel tiltSequencerPanel;

    private MuteSequencerPanel muteSequencerPanel;

    private MelodicSequencerGridPanel gridPanel;

    private ScalePanel scalePanel;

    private MelodicSequencerGeneratorPanel generatorPanel;

    private JLabel instrumentInfoLabel;

    private boolean updatingUI = false;

    /**
     * Constructor for MelodicSequencerPanel
     */
    private final MelodicSequencerPresenter presenter;

    public MelodicSequencerPanel(int index, Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer with properly assigned channel
        sequencer = MelodicSequencerManager.getInstance().newSequencer(index);

        // Set up the note event listener
        sequencer.setNoteEventListener(noteEventConsumer);

        // Set up step update listener with DIRECT callback (no CommandBus)
        sequencer.setStepUpdateListener(event -> updateStepHighlighting(event.oldStep(), event.newStep()));

        // Apply instrument preset immediately to ensure correct sound
        SoundbankManager.getInstance().applyInstrumentPreset(sequencer.getPlayer());

        presenter = new MelodicSequencerPresenter(sequencer, this);

        // Initialize the UI
        initialize();

        // Try to load the first sequence for this sequencer
        loadFirstSequenceIfExists();

        CommandBus.getInstance().register(presenter, new String[]{
                Commands.DRUM_PAD_SELECTED,
                Commands.DRUM_STEP_SELECTED,
                Commands.DRUM_INSTRUMENTS_UPDATED,
                Commands.HIGHLIGHT_STEP,
                Commands.TIMING_UPDATE,
                Commands.DRUM_PAD_SELECTED,
                Commands.PLAYER_SELECTION_EVENT,
                Commands.TRANSPORT_STOP,
                Commands.MELODIC_SEQUENCE_LOADED,    // Add these events
                Commands.MELODIC_SEQUENCE_UPDATED,
                Commands.MELODIC_SEQUENCE_CREATED,
                Commands.SCALE_SELECTED,
                Commands.ROOT_NOTE_SELECTED,
                Commands.PATTERN_UPDATED,
                Commands.INSTRUMENT_CHANGED
        });
    }

    /**
     * Attempts to load the first sequence for this sequencer if any exist
     */
    private void loadFirstSequenceIfExists() {
        // Check if the sequencer has an ID (it should, but verify)
        if (sequencer.getId() == null) {
            logger.warn("Cannot load first sequence - sequencer has no ID");
            return;
        }

        try {
            // Check if this sequencer has any sequences
            if (MelodicSequencerManager.getInstance().hasSequences(sequencer.getId())) {
                Long firstId = MelodicSequencerManager.getInstance().getFirstSequenceId(sequencer.getId());

                if (firstId != null) {
                    MelodicSequenceData data = RedisService.getInstance().findMelodicSequenceById(firstId,
                            sequencer.getId());

                    // Check if data was actually found
                    if (data == null) {
                        logger.warn("No sequence data found for ID {} and sequencer {}, creating new pattern",
                                firstId, sequencer.getId());
                        // Handle case where no data is found - create a new sequence
                        data = RedisService.getInstance().newMelodicSequence(sequencer.getId());
                    }

                    // Apply the sequence data
                    RedisService.getInstance().applyMelodicSequenceToSequencer(data, sequencer);

                    // Force UI update
                    SwingUtilities.invokeLater(() -> {
                        // Regular UI sync
                        syncUIWithSequencer();

                        // Be sure to update tilt panel after UI sync
                        if (tiltSequencerPanel != null) {
                            tiltSequencerPanel.syncWithSequencer();
                        }

                        CommandBus.getInstance().publish(Commands.SEQUENCER_SYNC_MESSAGE, this, sequencer);
                    });

                    // Notify that a pattern was loaded
                    CommandBus.getInstance().publish(
                            Commands.MELODIC_SEQUENCE_LOADED,
                            this,
                            new MelodicSequencerEvent(
                                    sequencer.getId(),
                                    data.getId()));

                    // If we have a navigation panel, update its display
                    if (navigationPanel != null) {
                        navigationPanel.updateSequenceIdDisplay();
                    }
                } else {
                    // No sequence ID found, create a new one
                    createDefaultSequence();
                }
            } else {
                logger.info("No saved sequences found for sequencer {}, creating default pattern",
                        sequencer.getId());
                createDefaultSequence();
            }
        } catch (Exception e) {
            // Handle any exceptions during loading
            logger.error("Error loading first sequence: {}", e.getMessage(), e);
            createDefaultSequence();
        }
    }

    /**
     * Creates a default sequence when none exists
     */
    private void createDefaultSequence() {
        try {
            // Create a new sequence using the Redis service
            MelodicSequenceData newData = RedisService.getInstance().newMelodicSequence(sequencer.getId());

            // Apply it to the sequencer
            RedisService.getInstance().applyMelodicSequenceToSequencer(newData, sequencer);

            // Update UI
            SwingUtilities.invokeLater(() -> {
                syncUIWithSequencer();
                if (tiltSequencerPanel != null) {
                    tiltSequencerPanel.syncWithSequencer();
                }

                CommandBus.getInstance().publish(Commands.SEQUENCER_SYNC_MESSAGE, this, sequencer);
            });

            // Notify that a new sequence was created
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_CREATED,
                    this,
                    new MelodicSequencerEvent(sequencer.getId(), newData.getId()));

            logger.info("Created default sequence for sequencer {}", sequencer.getId());
        } catch (Exception e) {
            logger.error("Error creating default sequence: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize the panel
     */
    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        setLayout(new BorderLayout(2, 2));
        UIHelper.setPanelBorder(this);

        JPanel westPanel = new JPanel(new BorderLayout(2, 2));

        JPanel eastPanel = new JPanel(new BorderLayout(2, 2));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel topPanel = new JPanel(new BorderLayout(2, 2));
        // Create sequence navigation panel
        navigationPanel = new MelodicSequenceNavigationPanel(sequencer);

        // Create sequence parameters panel
        sequenceParamsPanel = new MelodicSequenceParametersPanel(sequencer);

        // Navigation panel goes NORTH-WEST
        westPanel.add(navigationPanel, BorderLayout.EAST);

        // Sound parameters go NORTH-EAST
        eastPanel.add(new SoundParametersPanel(), BorderLayout.NORTH);

        // Create and add the center info panel with instrument info
        JPanel centerPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout for true vertical centering

        // Create the instrument info label
        // instrumentInfoLabel = new JLabel();
        // instrumentInfoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        // updateInstrumentInfoLabel(); // Initialize with current values

        // Add constraints to center vertically and horizontally
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0; // Give horizontal weight
        gbc.weighty = 1.0; // Give vertical weight - this is key for vertical centering
        // centerPanel.add(instrumentInfoLabel, gbc);

        // Add centerPanel to the top panel
        UIHelper.addSafely(topPanel, centerPanel, BorderLayout.CENTER);

        // Add panels to the top panel
        topPanel.add(westPanel, BorderLayout.EAST);
        topPanel.add(eastPanel, BorderLayout.WEST);

        // Add top panel to main layout
        header.add(topPanel);
        add(header, BorderLayout.NORTH);

        // Create the grid panel and add to center
        gridPanel = new MelodicSequencerGridPanel(sequencer);
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        JPanel sequencersPanel = new JPanel(new BorderLayout(2, 1));

        // Create the tilt panel and add it to the NORTH of bottom panel

        tiltSequencerPanel = new TiltSequencerPanel(sequencer);
        header.add(tiltSequencerPanel);
        muteSequencerPanel = new MuteSequencerPanel(sequencer);
        header.add(muteSequencerPanel);

        header.add(new OffsetSequencerPanel2(getSequencer()));

        // Create bottom panel with BorderLayout for proper positioning
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 1));

        bottomPanel.add(sequencersPanel, BorderLayout.NORTH);

        // Add the parameters panel to the CENTER of the bottom panel
        bottomPanel.add(sequenceParamsPanel, BorderLayout.CENTER);

        // Create right panel for additional controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1));

        // Create and add the scale panel to the RIGHT of the bottom panel
        scalePanel = new ScalePanel(sequencer);
        rightPanel.add(scalePanel);

        generatorPanel = new MelodicSequencerGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        // Add swing panel to the right panel
        swingPanel = new MelodicSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        // Add the right panel to the bottom panel's EAST region
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // Add the bottom panel to the SOUTH region of the main panel
        // add(bottomPanel, BorderLayout.SOUTH);
        header.add(bottomPanel);

        JPanel buttonPanel = new JPanel();
        UIHelper.setWidgetPanelBorder(buttonPanel, "Debug");

        // Create refresh button
        JButton refreshButton = new JButton(Symbols.get(Symbols.REFRESH));
        refreshButton.setToolTipText("Refresh all instrument presets");
        refreshButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        refreshButton.addActionListener(e -> CommandBus.getInstance().publish(
                Commands.REFRESH_ALL_INSTRUMENTS,
                this,
                sequencer));
        buttonPanel.add(refreshButton);
        buttonPanel.add(createInstrumentRefreshButton());
        buttonPanel.add(createRefreshButton());
        // Add the button to the bottom panel
        //westPanel.add(buttonPanel, BorderLayout.WEST);
    }

    // Add this as a new method:
    private JButton createInstrumentRefreshButton() {
        JButton refreshButton = new JButton(Symbols.get(Symbols.MIDI));
        refreshButton.setToolTipText("Refresh all instrument sounds (fixes sound issues)");

        refreshButton.addActionListener(e -> {
            // First melodic sequencers
            for (MelodicSequencer seq : MelodicSequencerManager
                    .getInstance().getAllSequencers()) {
                if (seq.getPlayer() != null && seq.getPlayer().getInstrument() != null) {
                    SoundbankManager.getInstance().applyInstrumentPreset(seq.getPlayer());
                }
            }

            // Then drum sequencers
            for (com.angrysurfer.core.sequencer.DrumSequencer seq : com.angrysurfer.core.service.DrumSequencerManager
                    .getInstance().getAllSequencers()) {
                for (com.angrysurfer.core.model.Player player : seq.getPlayers()) {
                    if (player != null && player.getInstrument() != null) {
                        SoundbankManager.getInstance().applyInstrumentPreset(player);
                    }
                }
                seq.ensureDeviceConnections();
            }

            // Notify user
            com.angrysurfer.core.api.CommandBus.getInstance().publish(
                    com.angrysurfer.core.api.Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(
                            "Sound Refresh", "Info", "Refreshed all instrument presets"));
        });

        return refreshButton;
    }

    private JButton createRefreshButton() {
        JButton refreshButton = new JButton(Symbols.get(Symbols.AUDIO));
        refreshButton.setToolTipText("Refresh instrument preset (fixes sound issues)");
        refreshButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH,
                UIHelper.CONTROL_HEIGHT));
        refreshButton.setMaximumSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH,
                UIHelper.CONTROL_HEIGHT));

        refreshButton.addActionListener(e -> {
            if (sequencer != null && sequencer.getPlayer() != null &&
                    sequencer.getPlayer().getInstrument() != null) {

                // Log current instrument state
                com.angrysurfer.core.model.InstrumentWrapper instr = sequencer.getPlayer().getInstrument();
                logger.info("Refreshing instrument: {} (bank={}, program={})",
                        instr.getName(), instr.getBankIndex(), instr.getPreset());

                // Send the player-specific refresh event
                PlayerRefreshEvent event = new PlayerRefreshEvent(this, sequencer.getPlayer());
                com.angrysurfer.core.api.CommandBus.getInstance().publish(
                        com.angrysurfer.core.api.Commands.PLAYER_REFRESH_EVENT,
                        this,
                        event
                );

                // Update UI
                com.angrysurfer.core.api.CommandBus.getInstance().publish(
                        com.angrysurfer.core.api.Commands.STATUS_UPDATE,
                        this,
                        new com.angrysurfer.core.api.StatusUpdate(
                                "Preset Refresh", "Info", "Refreshed instrument for " + sequencer.getPlayer().getName())
                );
            }
        });

        return refreshButton;
    }

    /**
     * Update step highlighting in the grid panel
     */
    private void updateStepHighlighting(int oldStep, int newStep) {
        // Delegate to grid panel
        if (gridPanel != null) {
            gridPanel.updateStepHighlighting(oldStep, newStep);
        }
    }

    public ScalePanel getScalePanel() {
        return scalePanel;
    }

    /**
     * Synchronize all UI elements with the current sequencer state
     */

    public void syncUIWithSequencer() {
        updatingUI = true;
        try {
            // Update sequence parameters panel
            if (sequenceParamsPanel != null)
                sequenceParamsPanel.updateUI(sequencer);

            if (scalePanel != null)
                scalePanel.updateUI(sequencer);

            if (gridPanel != null)
                gridPanel.syncWithSequencer();

            if (tiltSequencerPanel != null)
                tiltSequencerPanel.syncWithSequencer();

            if (swingPanel != null)
                swingPanel.updateControls();

            if (navigationPanel != null)
                navigationPanel.updateSequenceIdDisplay();

            if (latchToggleButton != null)
                latchToggleButton.setSelected(sequencer.isLatchEnabled());

            if (generatorPanel != null)
                generatorPanel.syncWithSequencer();

            CommandBus.getInstance().publish(Commands.SEQUENCER_SYNC_MESSAGE, this, sequencer);

        } finally {
            updatingUI = false;
        }

        revalidate();
        repaint();
    }

    /**
     * Modify onAction to prevent the infinite scale selection loop
     */

}
