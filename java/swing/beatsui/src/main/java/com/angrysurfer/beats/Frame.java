package com.angrysurfer.beats;

import com.angrysurfer.beats.panel.MainPanel;
// import com.angrysurfer.beats.panel.internalsynth.InternalSynthControlPanel;
import com.angrysurfer.beats.panel.session.SessionPanel;
import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class Frame extends JFrame implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Frame.class.getName());

    private final Map<Character, Integer> keyNoteMap;
    // private BackgroundPanel backgroundPanel;
    private MainPanel mainPanel;

    public Frame() {
        super("Beats");
        this.keyNoteMap = setupKeyMap();

        // Create background panel first
        // backgroundPanel = new BackgroundPanel();
        // backgroundPanel.setLayout(new BorderLayout());
        // setContentPane(backgroundPanel);

        setupFrame();
        setupMainContent();
        setupKeyboardManager();

        // Initialize DialogService with this frame
        DialogManager.initialize(this);
    }

    public void loadFrameState() {
        logger.info("Loading frame state for window");
        FrameState state = RedisService.getInstance().loadFrameState(Constants.APPLICATION_FRAME);
        logger.info("Frame state loaded: " + (state != null));

        if (state != null) {
            setSize(state.getFrameSizeX(), state.getFrameSizeY());
            setLocation(state.getFramePosX(), state.getFramePosY());

            try {
                setSelectedTab(state.getSelectedTab());
            } catch (Exception e) {
                logger.warn("Error setting selected tab: " + e.getMessage());
            }

            // Restore window state
            if (state.isMaximized()) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else if (state.isMinimized()) {
                setExtendedState(JFrame.ICONIFIED);
            } else {
                setExtendedState(JFrame.NORMAL);
            }

            logger.info("Applied frame state: " +
                    "size=" + state.getFrameSizeX() + "x" + state.getFrameSizeY() +
                    ", pos=" + state.getFramePosX() + "," + state.getFramePosY() +
                    ", tab=" + state.getSelectedTab() +
                    ", maximized=" + state.isMaximized() +
                    ", minimized=" + state.isMinimized());
        }

        // Add window listener for saving state on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveFrameState();
            }
        });
    }

    public void saveFrameState() {
        try {
            FrameState currentState = new FrameState();
            currentState.setSelectedTab(getSelectedTab());

            // Save normal window bounds even when maximized
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH || getExtendedState() == JFrame.ICONIFIED) {
                currentState.setFrameSizeX((int) getPreferredSize().getWidth());
                currentState.setFrameSizeY((int) getPreferredSize().getHeight());
                // currentState.setFramePosX(getX());
                // currentState.setFramePosY(getY());
            } else {
                currentState.setFrameSizeX(getWidth());
                currentState.setFrameSizeY(getHeight());
                currentState.setFramePosX(getX());
                currentState.setFramePosY(getY());
            }

            // Save window state
            currentState.setMaximized(getExtendedState() == JFrame.MAXIMIZED_BOTH);
            currentState.setMinimized(getExtendedState() == JFrame.ICONIFIED);
            currentState.setLookAndFeelClassName(UIManager.getLookAndFeel().getClass().getName());

            logger.info("Saving frame state: " +
                    "size=" + getWidth() + "x" + getHeight() +
                    ", pos=" + getX() + "," + getY() +
                    ", tab=" + getSelectedTab() +
                    ", maximized=" + currentState.isMaximized() +
                    ", minimized=" + currentState.isMinimized());

            RedisService.getInstance().saveFrameState(currentState, Constants.APPLICATION_FRAME);
        } catch (Exception e) {
            logger.error("Error saving frame state: " + e.getMessage());
        }
    }

    private void setupFrame() {
        setPreferredSize(new Dimension(1200, 800));
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Add component listener for resize and move events
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (isShowing()) { // Only handle if window is visible
                    CommandBus.getInstance().publish(Commands.WINDOW_RESIZED, this);
                    saveFrameState();
                }
            }

            public void componentMoved(java.awt.event.ComponentEvent e) {
                if (isShowing() && getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                    // Don't save position when maximized
                    saveFrameState();
                }
            }
        });

        // Make main content panel transparent
        // backgroundPanel.setBackground(new Color(245, 245, 245, 200)); // Light
        // background with some transparency

        setJMenuBar(new MenuBar(this));
        add(new ToolBar(), BorderLayout.NORTH);
        add(new StatusBar(), BorderLayout.SOUTH);
    }

    private Map<Character, Integer> setupKeyMap() {
        Map<Character, Integer> map = new HashMap<>();
        // White keys
        map.put('z', 60); // Middle C
        map.put('x', 62); // D
        map.put('c', 64); // E
        map.put('v', 65); // F
        map.put('b', 67); // G
        map.put('n', 69); // A
        map.put('m', 71); // B
        // Black keys
        map.put('s', 61); // C#
        map.put('d', 63); // D#
        map.put('g', 66); // F#
        map.put('h', 68); // G#
        map.put('j', 70); // A#
        return map;
    }

    private void setupKeyboardManager() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // Only process KEY_PRESSED events to avoid duplicate handling
                if (e.getID() != KeyEvent.KEY_PRESSED) {
                    return false;
                }

                // Check for modal dialogs - don't handle keys when dialogs are showing
                if (isModalDialogShowing()) {
                    return false;
                }

                // Handle spacebar for transport control
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    // Don't intercept space if typing in a text field
                    if (e.getComponent() instanceof javax.swing.text.JTextComponent) {
                        return false;
                    }

                    logger.info("Spacebar pressed - Toggling transport");
                    CommandBus.getInstance().publish(Commands.TOGGLE_TRANSPORT, this);
                    e.consume();
                    return true;
                }

                // Get the pressed key character
                char keyChar = Character.toLowerCase(e.getKeyChar());
                boolean keyMapped = keyNoteMap.containsKey(keyChar);

                if (!keyMapped) {
                    return false; // Not a mapped key, let the event pass through
                }

                // First check if internal synth tab is active
                if (mainPanel != null && isInternalSynthTabActive()) {
                    // Play note on the internal synth
                    int baseNote = keyNoteMap.get(keyChar);

                    // Determine command based on shift key
                    String command = e.isShiftDown() ? Commands.KEY_HELD : Commands.KEY_PRESSED;
                    CommandBus.getInstance().publish(command, this, baseNote);

                    // Consume the event
                    e.consume();
                    return true;
                }
                // Handle SessionPanel (existing code)
                else if (mainPanel != null && mainPanel.getSelectedComponent() instanceof SessionPanel) {
                    // Existing SessionPanel handling code...
                    if (keyChar == 'a') {
                        // Special case for 'a' key handling...

                        if (mainPanel.getPlayer() != null && mainPanel.getPlayer().getRootNote() != null) {
                            int playerNote = mainPanel.getPlayer().getRootNote().intValue();
                            logger.info("A key pressed - Playing active player's note: " + playerNote);

                            // Determine command based on shift key
                            String command = e.isShiftDown() ? Commands.KEY_HELD : Commands.KEY_PRESSED;
                            CommandBus.getInstance().publish(command, this, playerNote);

                            // Consume the event
                            e.consume();
                            return true;
                        }
                    } else if (keyNoteMap.containsKey(keyChar)) {
                        // Existing code for handling piano keys...
                        int baseNote = keyNoteMap.get(keyChar);

                        // Adjust for active player's octave...
                        int noteToPlay = baseNote;

                        if (mainPanel.getPlayer() != null) {
                            int playerOctave = mainPanel.getPlayer().getRootNote().intValue() / 12;
                            int baseOctave = 5; // Default keyboard mapping is in octave 5

                            // Adjust the note by the octave difference
                            noteToPlay = baseNote + ((playerOctave - baseOctave) * 12);

                            // Ensure within valid MIDI range
                            noteToPlay = Math.max(0, Math.min(127, noteToPlay));
                            logger.info("Key " + keyChar + " mapped to note " + noteToPlay +
                                    " (player octave: " + playerOctave + ")");
                        }

                        String command = e.isShiftDown() ? Commands.KEY_HELD : Commands.KEY_PRESSED;
                        CommandBus.getInstance().publish(command, this, noteToPlay);

                        // Consume the event
                        e.consume();
                        return true;
                    }
                }
                // Handle X0XPanel (existing code)
                else if (mainPanel != null && keyNoteMap.containsKey(keyChar)) {
                    // Find X0XPanel within the component hierarchy
                    Component selected = mainPanel.getSelectedComponent();
                    MainPanel x0xPanel = findX0XPanel(selected);

                    if (x0xPanel != null) {
                        // Get base note (for octave 5)
                        int baseNote = keyNoteMap.get(keyChar);

                        // Apply default velocity and duration based on Shift key
                        int velocity = e.isShiftDown() ? 110 : 90;
                        int durationMs = e.isShiftDown() ? 500 : 250;

                        // Play the note directly on X0X synthesizer
                        logger.info("Playing note {} on X0X synthesizer", baseNote);

                        // Use Timer to avoid holding the EDT during note playback
                        Timer noteTimer = new Timer(5, evt -> {
                            x0xPanel.playNote(baseNote, velocity, durationMs);
                            ((Timer) evt.getSource()).stop();
                        });
                        noteTimer.setRepeats(false);
                        noteTimer.start();

                        e.consume();
                        return true;
                    }
                }

                return false;
            }
        });
    }

    /**
     * Check if the Internal Synth tab is currently active
     */
    private boolean isInternalSynthTabActive() {
        // TEMPORARILY DISABLED - InternalSynthControlPanel
        return false;
        // Find if the active tab contains the InternalSynthControlPanel
        // Component selectedComponent = mainPanel.getSelectedComponent();
        // return selectedComponent instanceof InternalSynthControlPanel ||
        //         (selectedComponent != null && findChildOfType(selectedComponent, InternalSynthControlPanel.class) != null);
    }

    /**
     * Find a child component of specific type within a container
     */
    private <T> T findChildOfType(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }

        if (component instanceof Container container) {
            for (int i = 0; i < container.getComponentCount(); i++) {
                T result = findChildOfType(container.getComponent(i), type);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Recursively find the X0XPanel within a component hierarchy
     */
    private MainPanel findX0XPanel(Component component) {
        if (component == null) {
            return null;
        }

        if (component instanceof MainPanel) {
            return (MainPanel) component;
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                MainPanel panel = findX0XPanel(child);
                if (panel != null) {
                    return panel;
                }
            }
        }
        return null;
    }

    // Helper method to check if any modal dialog is showing
    private boolean isModalDialogShowing() {
        // Check if any modal dialogs are active
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window instanceof JDialog && ((JDialog) window).isModal()) {
                return true;
            }
        }
        return false;
    }

    private void setupMainContent() {
        mainPanel = new MainPanel();
        add(mainPanel, BorderLayout.CENTER);

        // Replace direct access with command
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                CommandBus.getInstance().publish(Commands.WINDOW_CLOSING, this);
                saveFrameState();
            }
        });
    }

    public <T> Dialog<T> createDialog(T data, JPanel content) {
        logger.info("Creating dialog with content: " + content.getClass().getSimpleName());
        Dialog<T> dialog = new Dialog<>(this, data, content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        logger.info("Dialog created and positioned");
        return dialog;
    }

    public int getSelectedTab() {
        return mainPanel.getSelectedTab();
    }

    public void setSelectedTab(int index) {
        mainPanel.setSelectedTab(index);
    }

    @Override
    public void close() {
        // Clean up ActionBus subscriptions for child components
        if (mainPanel != null) {
            try {
                mainPanel.close();
            } catch (Exception e) {
                logger.error("Error closing main panel: " + e.getMessage());
            }
        }
        dispose();
    }


}
