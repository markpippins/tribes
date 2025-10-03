package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.panel.instrument.InstrumentsPanel;
import com.angrysurfer.beats.panel.internalsynth.InternalSynthControlPanel;
import com.angrysurfer.beats.panel.modulation.QuadXYPadPanel;
import com.angrysurfer.beats.panel.modulation.TuringMachinePanel;
import com.angrysurfer.beats.panel.modulation.oscillator.LFOPanel;
import com.angrysurfer.beats.panel.sample.SampleBrowserPanel;
import com.angrysurfer.beats.panel.sequencer.SongPanel;
import com.angrysurfer.beats.panel.sequencer.mono.MelodicSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.poly.DrumEffectsSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.poly.DrumParamsSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.poly.DrumSequencerGridPanel;
import com.angrysurfer.beats.panel.session.SessionPanel;
import com.angrysurfer.beats.widget.CircleOfFifthsDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.StepUpdateEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.ChannelManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainPanel extends LivePanel implements AutoCloseable, IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class.getName());

    static {
        // Enable trace logging for CommandBus events
        System.setProperty("org.slf4j.simpleLogger.log.com.angrysurfer.core.api.CommandBus", "debug");
    }

    private final MelodicSequencerPanel[] melodicPanels = new MelodicSequencerPanel[SequencerConstants.MELODIC_CHANNELS.length];
    private JTabbedPane tabbedPane;
    private DrumSequencerGridPanel drumSequencerGridPanel;
    private DrumParamsSequencerPanel drumParamsSequencerPanel;
    private DrumEffectsSequencerPanel drumEffectsSequencerPanel;
    private GlobalMuteButtonsPanel muteButtonsPanel;

    private JTabbedPane drumsTabbedPane;

    private JTabbedPane melodicTabbedPane;

    private Point dragStartPoint;

    public MainPanel() {
        super();
        setLayout(new BorderLayout());
        CommandBus.getInstance().register(this, new String[]{
                Commands.SEQUENCER_STEP_UPDATE, Commands.SCALE_SELECTED, Commands.TOGGLE_TRANSPORT, Commands.ROOT_NOTE_SELECTED,
                Commands.SESSION_UPDATED, Commands.TRANSPORT_STOP, Commands.TRANSPORT_START
        });
        setupTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
    }

    private static JSplitPane createSplitPane(InstrumentsPanel instrumentsPanel, SystemsPanel systemsPanel) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(instrumentsPanel);
        splitPane.setBottomComponent(systemsPanel);

        // Set initial divider position (70% for instruments, 30% for systems)
        splitPane.setDividerLocation(0.7);
        splitPane.setResizeWeight(0.7); // Keep 70% proportion on resize

        // Make the divider slightly more visible
        splitPane.setDividerSize(8);

        // Remove any borders from the split pane itself
        splitPane.setBorder(null);
        return splitPane;
    }

    private void setupTabbedPane() {
        tabbedPane = new JTabbedPane();

        InternalSynthControlPanel internalSynthControlPanel = new InternalSynthControlPanel();
        tabbedPane.addTab("Multi", createDrumSequencersPanel());

        tabbedPane.addTab("Melo", createMelodicSequencersPanel());

        tabbedPane.addTab("Song", createSongPanel());
        tabbedPane.addTab("Mixer", createMixerPanel());
        tabbedPane.addTab("Synth", internalSynthControlPanel);
        tabbedPane.addTab("Matrix", createModulationMatrixPanel());
        tabbedPane.addTab("Players", new SessionPanel());

        JPanel pianoPanel = new JPanel(new BorderLayout());
        pianoPanel.add(new PianoPanel(), BorderLayout.SOUTH);

        tabbedPane.addTab("Launch", new LaunchPanel());

        tabbedPane.addTab("Samples", createSampleBrowserPanel());
        tabbedPane.addTab("Instruments", createCombinedInstrumentsSystemPanel());

        tabbedPane.addTab("Logs", new LoggingPanel());
        tabbedPane.addTab("Sandbox", createSandbox());
        // tabbedPane.addTab("Visualizer", new GridPanel());

        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        JPanel tabToolbar = new JPanel();
        tabToolbar.setLayout(new BoxLayout(tabToolbar, BoxLayout.X_AXIS));
        tabToolbar.setOpaque(false);

        tabToolbar.add(Box.createVerticalGlue());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Add mix button first
        // buttonPanel.add(new TransportIndicatorPanel());
        buttonPanel.add(createMixButton());

        // Add existing control buttons
        buttonPanel.add(createAllNotesOffButton());
        buttonPanel.add(createLoopToggleButton()); // Add the new loop toggle button
        buttonPanel.add(createMetronomeToggleButton());

        // buttonPanel.add(createRestartButton());

        // Create mute buttons toolbar early
        JPanel muteButtonsToolbar = createMuteButtonsToolbar();

        // Add the mute buttons toolbar
        add(muteButtonsToolbar, BorderLayout.NORTH);
        // add(new SoundParametersPanel(), BorderLayout.SOUTH);

        tabToolbar.add(Box.createHorizontalStrut(10));

        tabToolbar.add(buttonPanel);
        tabToolbar.add(Box.createVerticalGlue());

        tabbedPane.putClientProperty("JTabbedPane.trailingComponent", tabToolbar);

        // Add mouse motion listener for drag-and-drop functionality
        tabbedPane.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Store the drag start point
                dragStartPoint = e.getLocationOnScreen();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (tabIndex >= 0) {
                    JComponent comp = (JComponent) tabbedPane.getComponentAt(tabIndex);

                    Point p = e.getLocationOnScreen();
                    // Check if dragged far enough from original position
                    if (isDraggedFarEnough(p, dragStartPoint)) {
                        // Create new frame containing component from this tab
                        String title = tabbedPane.getTitleAt(tabIndex);
                        createDetachedWindow(comp, title, p);

                        // Remove the tab from original pane
                        tabbedPane.remove(tabIndex);
                    }
                }
            }
        });

        addListeners(tabbedPane);
        addListeners(melodicTabbedPane);
        addListeners(drumsTabbedPane);

        updateMuteButtonSequencers();
    }

    private JPanel createSandbox() {
        JPanel panel = new JPanel(new BorderLayout());

        CircleOfFifthsDial dial = new CircleOfFifthsDial();
        dial.setMinimumSize(new Dimension(250, 250));
        dial.setPreferredSize(new Dimension(250, 250));
        dial.setMaximumSize(new Dimension(250, 250));

        panel.add(dial, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Recursively adds listeners to all tabbedPanes and their nested components
     * to ensure focus handling and player activation work correctly
     *
     * @param component The component to process (starts with main tabbedPane)
     */
    private void addListenersRecursive(Component component) {
        // Process JTabbedPane components
        if (component instanceof JTabbedPane pane) {

            // Add change listener to this tabbed pane
            pane.addChangeListener(e -> {
                // Get the selected component
                Component selectedComponent = tabbedPane.getSelectedComponent();
                handleComponentSelection(selectedComponent, tabbedPane);
            });

            // Process each child component in the tabbedPane
            for (int i = 0; i < pane.getTabCount(); i++) {
                Component tabComponent = pane.getComponentAt(i);
                // Recursively process this component
                addListenersRecursive(tabComponent);
            }
        }
        // Process any container that might contain other components
        else if (component instanceof Container container) {

            // Process all child components
            for (Component child : container.getComponents()) {
                addListenersRecursive(child);
            }
        }
    }

    /**
     * Unified method to handle component selection in any tabbed pane
     *
     * @param selectedComponent The component that was selected
     * @param sourceTabbedPane  The tabbed pane where selection occurred
     */
    private void handleComponentSelection(Component selectedComponent, JTabbedPane sourceTabbedPane) {
        if (selectedComponent == null)
            return;

        // Request focus on the newly selected tab component
        SwingUtilities.invokeLater(selectedComponent::requestFocusInWindow);

        // Case 1: DrumSequencerPanel direct selection
        if (selectedComponent == drumSequencerGridPanel) {
            // Add more comprehensive null checking
            if (drumSequencerGridPanel.getDrumSelectorPanel() != null &&
                    drumSequencerGridPanel.getDrumSelectorPanel().getSelectedDrumPadIndex() != null &&
                    drumSequencerGridPanel.getSequencer() != null) {

                int padIndex = drumSequencerGridPanel.getDrumSelectorPanel().getSelectedDrumPadIndex();
                Player player = drumSequencerGridPanel.getSequencer().getPlayer(padIndex);

                if (player != null) {
                    activatePlayer(player, "drum sequencer");
                } else {
                    logger.warn("No player found for drum pad index: {}", padIndex);
                }
            } else {
                // Handle the case when no pad is selected or sequencer isn't ready
                logger.debug("DrumSequencerPanel selected but no pad selected or sequencer not ready");
            }
        }

        // Case 2: DrumParamsSequencerPanel direct selection
        else if (selectedComponent == drumParamsSequencerPanel) {
            Player player = drumParamsSequencerPanel.getSequencer().getPlayer(
                    drumParamsSequencerPanel.getSelectedPadIndex());
            activatePlayer(player, "drum params");
        }

        // Case 3: DrumEffectsSequencerPanel direct selection
        else if (selectedComponent == drumEffectsSequencerPanel) {
            Player player = drumEffectsSequencerPanel.getSequencer().getPlayer(
                    drumEffectsSequencerPanel.getSelectedPadIndex());
            activatePlayer(player, "drum effects");
        }

        // Case 4: MelodicSequencerPanel direct selection
        else if (selectedComponent instanceof MelodicSequencerPanel) {
            Player player = ((MelodicSequencerPanel) selectedComponent).getSequencer().getPlayer();
            activatePlayer(player, "melodic");
        }

        // Case 5: Drums tabbed pane selection
        else if (selectedComponent == drumsTabbedPane) {
            Component selectedDrumsTab = drumsTabbedPane.getSelectedComponent();
            Player player = null;

            try {
                if (selectedDrumsTab instanceof DrumSequencerGridPanel drumPanel) {

                    if (drumPanel.getDrumSelectorPanel() != null &&
                            drumPanel.getDrumSelectorPanel().getSelectedDrumPadIndex() != null &&
                            drumPanel.getSequencer() != null) {

                        int playerIndex = drumPanel.getDrumSelectorPanel().getSelectedDrumPadIndex();
                        player = drumPanel.getSequencer().getPlayer(playerIndex);
                    }
                } else if (selectedDrumsTab instanceof DrumEffectsSequencerPanel effectsPanel) {
                    // Add null checks for other panels too

                    if (effectsPanel.getSelectedPadIndex() > -1 && effectsPanel.getSequencer() != null) {
                        int playerIndex = effectsPanel.getSelectedPadIndex();
                        player = effectsPanel.getSequencer().getPlayer(playerIndex);
                    }
                } else if (selectedDrumsTab instanceof DrumParamsSequencerPanel paramsPanel) {

                    if (paramsPanel.getSelectedPadIndex() > -1 && paramsPanel.getSequencer() != null) {
                        int playerIndex = paramsPanel.getSelectedPadIndex();
                        player = paramsPanel.getSequencer().getPlayer(playerIndex);
                    }
                }

                if (player != null) {
                    activatePlayer(player, "drums tabbed pane");
                } else {
                    logger.debug("No player available for selected drums tab");
                }
            } catch (Exception e) {
                logger.error("Error selecting drum tab: {}", e.getMessage(), e);
            }
        }

        // Case 6: Melodic tabbed pane selection
        else if (selectedComponent == melodicTabbedPane) {
            Component selectedMelodicTab = melodicTabbedPane.getSelectedComponent();
            MelodicSequencerPanel melodicPanel = findMelodicSequencerPanel(selectedMelodicTab);

            if (melodicPanel != null && melodicPanel.getSequencer() != null) {
                // Activate player as before
                Player player = melodicPanel.getSequencer().getPlayer();
                activatePlayer(player, "melodic tabbed pane");

                // Force UI refresh for the selected melodic tab
                // SwingUtilities.invokeLater(() -> {
                //     // Invalidate and repaint the entire panel
                //     melodicPanel.invalidate();
                //     melodicPanel.validate();
                //     melodicPanel.repaint();

                //     // Also refresh specific components that might need it
                //     if (melodicPanel.getSequenceNavigationPanel() != null) {
                //         melodicPanel.getSequenceNavigationPanel().invalidate();
                //         melodicPanel.getSequenceNavigationPanel().repaint();
                //     }

                //     logger.debug("Refreshed UI for melodic tab: {}", player.getName());
                // });
            }
        }

        // Handle more specialized tab selections that might need additional processing
        else if (sourceTabbedPane == tabbedPane) {
            // Handle main tabbed pane selection for specific tab indices
            int selectedIndex = tabbedPane.getSelectedIndex();
            String tabName = tabbedPane.getTitleAt(selectedIndex);
            logger.debug("Main tab selected: {} (index: {})", tabName, selectedIndex);

            // Additional custom handling for specific tabs could go here
        }
    }

    /**
     * Helper method to activate a player and publish event
     *
     * @param player The player to activate
     * @param source Description of the source for logging
     */
    private void activatePlayer(Player player, String source) {
        if (player != null) {
            CommandBus.getInstance().publish(
                    Commands.PLAYER_SELECTION_EVENT,
                    this,
                    player);
            logger.debug("Tab switched to {} - set player '{}' as active",
                    source, player.getName());
        }
    }

    /**
     * Update the addListeners method to use the recursive implementation
     */
    private void addListeners(JTabbedPane tabbedPane) {
        // Call the recursive implementation
        addListenersRecursive(tabbedPane);
    }

    private JTabbedPane createDrumSequencersPanel() {

        drumsTabbedPane = new JTabbedPane();
        // drumsTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        drumsTabbedPane.addTab("Sequencer", createDrumPanel());
        drumsTabbedPane.addTab("Parameters", createDrumParamsPanel());
        drumsTabbedPane.addTab("Mix", createDrumEffectsPanel());

        return drumsTabbedPane;
    }

    private JTabbedPane createMelodicSequencersPanel() {
        melodicTabbedPane = new JTabbedPane();
        // melodicTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

        // Initialize all melodic sequencer panels with proper channel distribution
        for (int i = 0; i < SequencerConstants.MELODIC_CHANNELS.length; i++) {
            melodicPanels[i] = createMelodicSequencerPanel(i);
            melodicTabbedPane.addTab("Melo " + (i + 1), melodicPanels[i]);
        }

        return melodicTabbedPane;
    }

    private boolean isDraggedFarEnough(Point currentPoint, Point startPoint) {
        if (startPoint == null) {
            return false;
        }
        int dx = currentPoint.x - startPoint.x;
        int dy = currentPoint.y - startPoint.y;
        return Math.sqrt(dx * dx + dy * dy) > 20; // Example threshold
    }

    /**
     * Creates a detached window from a tab component and handles reattachment when
     * closed
     */
    private void createDetachedWindow(JComponent comp, String title, Point location) {
        // Create a modeless dialog for the detached tab
        JDialog detachedWindow = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                Dialog.ModalityType.MODELESS);
        detachedWindow.setContentPane(comp);

        // Add window listener to handle reattachment when window is closed
        detachedWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logger.info("Reattaching tab: {}", title);

                // Remove component from dialog before adding back to tabbedPane
                detachedWindow.setContentPane(new JPanel());

                // Add the component back to the tabbed pane
                tabbedPane.addTab(title, comp);

                // Select the newly added tab
                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            }
        });

        // Size, position and display the window
        detachedWindow.pack();
        detachedWindow.setLocation(location);
        detachedWindow.setVisible(true);
    }

    private Component createSongPanel() {
        // Return instance of our new SongPanel
        return new SongPanel();
    }

    /**
     * Creates a combined panel with InstrumentsPanel and SystemsPanel in a vertical
     * JSplitPane
     */
    private JPanel createCombinedInstrumentsSystemPanel() {
        JPanel combinedPanel = new JPanel(new BorderLayout());

        // Create the component panels
        InstrumentsPanel instrumentsPanel = new InstrumentsPanel();
        SystemsPanel systemsPanel = new SystemsPanel();

        // Add titled border to systems panel for visual separation
        systemsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "MIDI Devices",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP));

        // Create a vertical JSplitPane (top-bottom arrangement)
        JSplitPane splitPane = createSplitPane(instrumentsPanel, systemsPanel);

        // Add the split pane to the combined panel
        combinedPanel.add(splitPane, BorderLayout.CENTER);

        return combinedPanel;
    }

    private Component createDrumPanel() {
        drumSequencerGridPanel = new DrumSequencerGridPanel(noteEvent -> {
            logger.debug("Drum note event received: note={}, velocity={}",
                    noteEvent.getNote(), noteEvent.getVelocity());

            // Publish to CommandBus so MuteButtonsPanel can respond
            // Subtract 36 to convert MIDI note to drum index (36=kick, etc.)
            int drumIndex = noteEvent.getNote() - 36;
            CommandBus.getInstance().publish(Commands.DRUM_NOTE_TRIGGERED, drumSequencerGridPanel.getSequencer(),
                    drumIndex);
        });
        return drumSequencerGridPanel;
    }

    private Component createDrumEffectsPanel() {
        drumEffectsSequencerPanel = new DrumEffectsSequencerPanel(noteEvent -> {
            // No-op for now
        });
        return drumEffectsSequencerPanel;
    }

    private Component createDrumParamsPanel() {
        drumParamsSequencerPanel = new DrumParamsSequencerPanel(noteEvent -> {
            // No-op for now
        });
        return drumParamsSequencerPanel;
    }

    private MelodicSequencerPanel createMelodicSequencerPanel(int index) {
        return new MelodicSequencerPanel(index, noteEvent -> {

            // Get the panel's sequencer to use as the event source
            MelodicSequencer sequencer = null;
            if (index >= 0 && index < melodicPanels.length && melodicPanels[index] != null) {
                sequencer = melodicPanels[index].getSequencer();
            }

            // Publish to CommandBus so MuteButtonsPanel can respond
            if (sequencer != null) {
                CommandBus.getInstance().publish(Commands.MELODIC_NOTE_TRIGGERED, sequencer, noteEvent);
            }
        });
    }

    private Component createModulationMatrixPanel() {
        JTabbedPane modulationTabbedPane = new JTabbedPane();

        // Create a main panel with a border
        JPanel lfoPanel = new JPanel(new BorderLayout(10, 10));
        lfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a panel with GridLayout (1 row, 3 columns) with spacing
        JPanel lfoBankPanel = new JPanel(new GridLayout(1, 3, 15, 0));

        // Create three LFO panels with distinct names
        LFOPanel lfo1 = new LFOPanel();
        lfo1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("LFO 1"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        LFOPanel lfo2 = new LFOPanel();
        lfo2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("LFO 2"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        LFOPanel lfo3 = new LFOPanel();
        lfo3.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("LFO 3"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Add the three LFO panels to the grid
        lfoBankPanel.add(lfo1);
        lfoBankPanel.add(lfo2);
        lfoBankPanel.add(lfo3);

        // Add the grid panel to the main panel
        lfoPanel.add(lfoBankPanel, BorderLayout.CENTER);

        // Add tabs to the modulation tabbed pane
        modulationTabbedPane.addTab("LFOs", lfoPanel);
        modulationTabbedPane.addTab("Complex LFO", new ComplexLFOPanel());
        modulationTabbedPane.addTab("XY Pad", createXYPadPanel());

        // Create a container panel for multiple Turing Machines
        JPanel turingMachinesContainer = new JPanel(new GridLayout(2, 3, 10, 10));
        turingMachinesContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create and add 6 independent Turing Machine panels
        for (int i = 0; i < 6; i++) {
            TuringMachinePanel panel = new TuringMachinePanel();
            // Modify each panel to use a more compact appearance for the grid layout
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("TM " + (i + 1)),
                    BorderFactory.createEmptyBorder(3, 3, 3, 3)));
            // Give each a unique identifier for command bus messages
            panel.setName("TuringMachine" + (i + 1));
            turingMachinesContainer.add(panel);
        }

        // Add the Turing Machines container to the tabbed pane
        modulationTabbedPane.addTab("Turing Machines", turingMachinesContainer);

        return modulationTabbedPane;
    }

    private QuadXYPadPanel createXYPadPanel() {
        return new QuadXYPadPanel();
    }

    private Component createMixerPanel() {
        return new MixerPanel(InternalSynthManager.getInstance().getSynthesizer());
    }

    private JPanel createMuteButtonsToolbar() {
        // Create the mute buttons panel
        muteButtonsPanel = new GlobalMuteButtonsPanel();

        // We'll update the sequencers after they're created
        return muteButtonsPanel;
    }

    private void updateMuteButtonSequencers() {
        // Set the drum sequencer
        if (drumSequencerGridPanel != null) {
            DrumSequencer drumSeq = drumSequencerGridPanel.getSequencer();
            muteButtonsPanel.setDrumSequencer(drumSeq);

            // *** THIS IS THE CRITICAL PART - Set up drum note event publisher ***
            logger.info("Setting up drum note event publisher");
            drumSeq.setNoteEventPublisher(noteEvent -> {
                int drumIndex = noteEvent.getNote() - 36; // Convert MIDI note to drum index
                logger.debug("Publishing drum note event: index={}, velocity={}",
                        drumIndex, noteEvent.getVelocity());
                CommandBus.getInstance().publish(
                        Commands.DRUM_NOTE_TRIGGERED,
                        drumSeq,
                        drumIndex);
            });
        }

        // Set the melodic sequencers
        List<MelodicSequencer> melodicSequencers = new ArrayList<>();
        for (MelodicSequencerPanel panel : melodicPanels) {
            if (panel != null) {
                MelodicSequencer seq = panel.getSequencer();
                melodicSequencers.add(seq);

                // *** THIS IS ALSO CRITICAL - Set up melodic note event publisher ***
                logger.info("Setting up melodic note event publisher for channel {}",
                        seq.getPlayer().getChannel());
                seq.setNoteEventPublisher(noteEvent -> {
                    logger.debug("Publishing melodic note event: note={}, velocity={}",
                            noteEvent.getNote(), noteEvent.getVelocity());
                    CommandBus.getInstance().publish(
                            Commands.MELODIC_NOTE_TRIGGERED,
                            seq,
                            noteEvent);
                });
            }
        }
        muteButtonsPanel.setMelodicSequencers(melodicSequencers);
    }

    public void playNote(int note, int velocity, int durationMs) {
        int activeMidiChannel = 15;
        InternalSynthManager.getInstance().playNote(note, velocity, durationMs, activeMidiChannel);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TRANSPORT_START -> {
                // isPlaying = true;
            }

            case Commands.TRANSPORT_STOP -> {
                // isPlaying = false;
            }

            case Commands.SESSION_UPDATED -> {
                // Nothing to do here - sequencer handles timing updates
            }

            case Commands.ROOT_NOTE_SELECTED -> {
                if (action.getData() instanceof String rootNote) {
                    // Handle root note selection if needed
                }
            }

            case Commands.SCALE_SELECTED -> {
                if (action.getData() instanceof String scaleName) {
                    // Handle scale selection if needed
                }
            }

            case Commands.SEQUENCER_STEP_UPDATE -> {
                if (action.getData() instanceof StepUpdateEvent stepUpdateEvent) {
                    int step = stepUpdateEvent.newStep();
                    // Handle step update if needed
                }
            }

            case Commands.TOGGLE_TRANSPORT -> {
                // Instead of manipulating sequencer directly, publish appropriate commands
                // logger.info("Toggling transport state (current state: {})", isPlaying ?
                // "playing" : "stopped");

                if (SessionManager.getInstance().getActiveSession().isRunning()) {
                    // If currently playing, publish stop command
                    logger.info("Publishing TRANSPORT_STOP command");
                    CommandBus.getInstance().publish(Commands.TRANSPORT_STOP, this);
                } else {
                    // If currently stopped, publish start command
                    logger.info("Publishing TRANSPORT_START command");
                    CommandBus.getInstance().publish(Commands.TRANSPORT_START, this);
                }

                // The state will be updated when we receive TRANSPORT_STARTED or
                // TRANSPORT_STOPPED events
            }
        }
    }

    @Override
    public void handlePlayerActivated() {

    }

    @Override
    public void handlePlayerUpdated() {

    }

    private JToggleButton createMetronomeToggleButton() {
        JToggleButton metronomeButton = new JToggleButton();
        metronomeButton.setText(Symbols.get(Symbols.METRONOME)); // Unicode metronome symbol
        // Set equal width and height to ensure square shape
        metronomeButton.setPreferredSize(new Dimension(28, 28));
        metronomeButton.setMinimumSize(new Dimension(28, 28));
        metronomeButton.setMaximumSize(new Dimension(28, 28));

        // Remove the rounded rectangle property
        // metronomeButton.putClientProperty("JButton.buttonType", "roundRect");

        // Explicitly set square size and enforce square shape
        metronomeButton.putClientProperty("JButton.squareSize", true);
        metronomeButton.putClientProperty("JComponent.sizeVariant", "regular");

        metronomeButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        metronomeButton.setHorizontalAlignment(SwingConstants.CENTER);
        metronomeButton.setVerticalAlignment(SwingConstants.CENTER);
        metronomeButton.setMargin(new Insets(0, 0, 0, 0));
        metronomeButton.setToolTipText("Toggle Metronome");

        metronomeButton.addActionListener(e -> {
            boolean isSelected = metronomeButton.isSelected();
            logger.info("Metronome toggled: {}", isSelected ? "ON" : "OFF");
            CommandBus.getInstance().publish(isSelected ? Commands.METRONOME_START : Commands.METRONOME_STOP, this);
        });

        CommandBus.getInstance().register(action -> {
            if (action.getCommand() == null)
                return;

            switch (action.getCommand()) {
                case Commands.METRONOME_STARTED:
                    SwingUtilities.invokeLater(() -> {
                        metronomeButton.setSelected(true);
                        metronomeButton.setBackground(Color.GREEN);
                        metronomeButton.invalidate();
                        metronomeButton.repaint();
                    });
                    break;

                case Commands.METRONOME_STOPPED:
                    SwingUtilities.invokeLater(() -> {
                        metronomeButton.setSelected(false);
                        metronomeButton.setBackground(Color.RED);
                        metronomeButton.invalidate();
                        metronomeButton.repaint();
                    });
                    break;
            }
        }, new String[]{Commands.METRONOME_STARTED, Commands.METRONOME_STOPPED});

        metronomeButton.setSelected(UserConfigManager.getInstance().getCurrentConfig().isMetronomeAudible());

        return metronomeButton;
    }

    private JToggleButton createLoopToggleButton() {
        JToggleButton loopButton = new JToggleButton();
        loopButton.setText(Symbols.get(Symbols.LOOP)); // Unicode loop symbol

        // Set equal width and height to ensure square shape
        loopButton.setPreferredSize(new Dimension(28, 28));
        loopButton.setMinimumSize(new Dimension(28, 28));
        loopButton.setMaximumSize(new Dimension(28, 28));

        // Explicitly set square size and enforce square shape
        loopButton.putClientProperty("JButton.squareSize", true);
        loopButton.putClientProperty("JComponent.sizeVariant", "regular");

        loopButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        loopButton.setHorizontalAlignment(SwingConstants.CENTER);
        loopButton.setVerticalAlignment(SwingConstants.CENTER);
        loopButton.setMargin(new Insets(0, 0, 0, 0));
        loopButton.setToolTipText("Toggle All Sequencer Looping");

        // Default to selected (looping enabled)
        loopButton.setSelected(true);

        loopButton.addActionListener(e -> {
            boolean isLooping = loopButton.isSelected();
            logger.info("Global looping toggled: {}", isLooping ? "ON" : "OFF");

            // Set looping state for drum sequencer
            if (drumSequencerGridPanel != null && drumSequencerGridPanel.getSequencer() != null) {
                drumSequencerGridPanel.getSequencer().setLooping(isLooping);
            }

            // Set looping state for all melodic sequencers
            for (MelodicSequencerPanel panel : melodicPanels) {
                if (panel != null && panel.getSequencer() != null) {
                    panel.getSequencer().getSequenceData().setLooping(isLooping);
                }
            }

            // Set looping state for drum params sequencer if present
            // if (drumParamssSequencerPanel != null &&
            // drumParamsSequencerPanel.getSequencer() != null) {
            // drumParamsSequencerPanel.getSequencer().setLooping(isLooping);
            // }

            // Visual feedback - change button color based on state
            loopButton.setBackground(isLooping ? new Color(120, 200, 120) : new Color(200, 120, 120));

            // Publish command for other components to respond to
            CommandBus.getInstance().publish(
                    isLooping ? Commands.GLOBAL_LOOPING_ENABLED : Commands.GLOBAL_LOOPING_DISABLED,
                    this);
        });

        // Initial button color - green for enabled looping
        loopButton.setBackground(new Color(120, 200, 120));

        return loopButton;
    }

    private JButton createAllNotesOffButton() {
        JButton notesOffButton = new JButton();
        notesOffButton.setText(Symbols.get(Symbols.ALL_NOTES_OFF)); // Unicode all notes off symbol
        // Set equal width and height to ensure square shape
        notesOffButton.setPreferredSize(new Dimension(28, 28));
        notesOffButton.setMinimumSize(new Dimension(28, 28));
        notesOffButton.setMaximumSize(new Dimension(28, 28));

        // Remove the rounded rectangle property
        // notesOffButton.putClientProperty("JButton.buttonType", "roundRect");

        // Explicitly set square size and enforce square shape
        notesOffButton.putClientProperty("JButton.squareSize", true);
        notesOffButton.putClientProperty("JComponent.sizeVariant", "regular");

        notesOffButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        notesOffButton.setHorizontalAlignment(SwingConstants.CENTER);
        notesOffButton.setVerticalAlignment(SwingConstants.CENTER);
        notesOffButton.setMargin(new Insets(0, 0, 0, 0));
        notesOffButton.setToolTipText("All Notes Off - Silence All Sounds");

        notesOffButton.addActionListener(e -> {
            logger.info("All Notes Off button pressed");
            CommandBus.getInstance().publish(Commands.ALL_NOTES_OFF, this);
        });

        return notesOffButton;
    }

    private JButton createRestartButton() {
        JButton restartButton = new JButton("Restart App");
        restartButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    "This will restart the application. Any unsaved changes will be lost.\nContinue?",
                    "Restart Application",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                try {
                    System.exit(0);

                    String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                    File currentJar = new File(
                            MainPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());

                    if (currentJar.getName().endsWith(".jar")) {
                        ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath());
                        builder.start();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "Error restarting: " + ex.getMessage(),
                            "Restart Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return restartButton;
    }

    /**
     * Create the sample browser panel
     */
    private JPanel createSampleBrowserPanel() {
        return new SampleBrowserPanel();
    }

    private JButton createMixButton() {
        JButton mixButton = new JButton();
        // Use a mixer icon character instead of text to fit in a square button
        // mixButton.setText("🎛️");
        mixButton.setText(Symbols.get(Symbols.MIX)); // Unicode mixer sy

        // Set equal width and height to ensure square shape
        mixButton.setPreferredSize(new Dimension(28, 28));
        mixButton.setMinimumSize(new Dimension(28, 28));
        mixButton.setMaximumSize(new Dimension(28, 28));

        // Remove the rounded rectangle property
        // mixButton.putClientProperty("JButton.buttonType", "roundRect");

        // Explicitly set square size and enforce square shape
        mixButton.putClientProperty("JButton.squareSize", true);
        mixButton.putClientProperty("JComponent.sizeVariant", "regular");

        // Style text to match other buttons
        mixButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 18));
        mixButton.setHorizontalAlignment(SwingConstants.CENTER);
        mixButton.setVerticalAlignment(SwingConstants.CENTER);
        mixButton.setMargin(new Insets(0, 0, 0, 0));
        mixButton.setToolTipText("Show Drum Mixer");

        // Add action listener to show mixer dialog
        mixButton.addActionListener(e -> {
            // Get current sequencer
            DrumSequencer sequencer = null;
            if (drumSequencerGridPanel != null) {
                sequencer = drumSequencerGridPanel.getSequencer();
            }

            if (sequencer != null) {
                // Create a new PopupMixerPanel and dialog each time
                PopupMixerPanel mixerPanel = new PopupMixerPanel(sequencer);

                // Create dialog to show the mixer
                JDialog mixerDialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                        "Pop-Up Mixer",
                        Dialog.ModalityType.MODELESS); // Non-modal dialog
                mixerDialog.setResizable(false);
                mixerDialog.setContentPane(mixerPanel);
                mixerDialog.pack();
                mixerDialog.setLocationRelativeTo(this);
                mixerDialog.setMinimumSize(new Dimension(600, 400));

                // Add window listener to handle dialog closing
                mixerDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        // Clean up any resources if needed
                        // (Optional) For example, remove any listeners registered to the mixer panel
                    }
                });

                mixerDialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No drum sequencer available",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return mixButton;
    }

    public int getSelectedTab() {
        return tabbedPane.getSelectedIndex();
    }

    public void setSelectedTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public Component getSelectedComponent() {
        return tabbedPane.getSelectedComponent();
    }

    @Override
    public void close() throws Exception {
        if (tabbedPane != null) {
            for (Component comp : tabbedPane.getComponents()) {
                if (comp instanceof LoggingPanel) {
                    ((LoggingPanel) comp).cleanup();
                } else if (comp instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) comp).close();
                    } catch (Exception e) {
                        logger.error("Error closing component: {}", e.getMessage());
                    }
                }
            }
        }

        // Release channels used by melodic panels
        for (MelodicSequencerPanel panel : melodicPanels) {
            if (panel != null && panel.getSequencer() != null) {
                int channel = panel.getSequencer().getPlayer().getChannel();
                ChannelManager.getInstance().releaseChannel(channel);
                logger.info("Released channel {} on application close", channel);
            }
        }
    }

    /**
     * Helper method to find MelodicSequencerPanel in component hierarchy
     */
    private MelodicSequencerPanel findMelodicSequencerPanel(Component component) {
        if (component instanceof MelodicSequencerPanel) {
            return (MelodicSequencerPanel) component;
        } else if (component instanceof Container container) {
            // Search through container's children recursively
            for (Component child : container.getComponents()) {
                MelodicSequencerPanel panel = findMelodicSequencerPanel(child);
                if (panel != null) {
                    return panel;
                }
            }
        }
        return null;
    }
}

