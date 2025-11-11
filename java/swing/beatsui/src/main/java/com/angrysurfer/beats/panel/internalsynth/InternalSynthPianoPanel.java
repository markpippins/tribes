package com.angrysurfer.beats.panel.internalsynth;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.ColorAnimator;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.sequencer.Scale;
import com.angrysurfer.core.service.MidiService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

@Getter
@Setter
public class InternalSynthPianoPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(InternalSynthPianoPanel.class);
    private static final int SYNTH_MIDI_CHANNEL = 15; // Internal synth uses channel 15 (16 in human numbering)
    private static final int OCTAVE_COUNT = 5; // Increased from 4 to 5 octaves for wider range
    private final Set<Integer> heldNotes = new HashSet<>();
    private final ColorAnimator colorAnimator;
    private String currentRoot = "C";
    private String currentScale = "Chromatic";
    private Map<Integer, JButton> noteToKeyMap = new HashMap<>();
    private JButton activeButton = null;
    private int baseOctave = 3; // Start at C3 (midi note 48)

    private JButton followScaleBtn;
    private JLabel playerStatusIndicator;

    public InternalSynthPianoPanel() {
        super(null); // Use null layout for precise key positioning

        // Calculate width based on octave count
        int whiteKeyWidth = 30;
        int totalWhiteKeys = 7 * OCTAVE_COUNT;
        int totalWidth = totalWhiteKeys * whiteKeyWidth + 50; // Extra space for controls

        setPreferredSize(new Dimension(totalWidth, 100));
        setMinimumSize(new Dimension(totalWidth, 100));
        setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        setOpaque(true);
        setBackground(UIHelper.fadedOrange);

        // Add control buttons
        setupControlButtons();

        // Create piano keys for multiple octaves
        createMultiOctavePiano();

        // Initialize color animator for visual effects
        colorAnimator = new ColorAnimator();
        colorAnimator.setOnColorUpdate(() -> repaint());
        colorAnimator.start();

        // Setup listeners
        setupActionBusListener();
        setupPlayerStatusIndicator();

        // Add keyboard navigation for octave shifting
        setupKeyboardNavigation();
    }

    private void setupKeyboardNavigation() {
        // Make the panel focusable so it can receive key events
        setFocusable(true);

        // Add key bindings for left and right arrow keys
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // SWAPPED: Left arrow now increases octave
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "octaveUp");
        actionMap.put("octaveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftOctaveUp();
            }
        });

        // SWAPPED: Right arrow now decreases octave
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "octaveDown");
        actionMap.put("octaveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftOctaveDown();
            }
        });

        // Request focus when the panel becomes visible
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                requestFocusInWindow();
            }
        });
    }

    private void shiftOctaveDown() {
        if (baseOctave > 0) {
            baseOctave--;
            recreatePiano();
            logger.info("Piano base octave changed to: {} (via keyboard)", baseOctave);

            // Show visual feedback - update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Octave: " + baseOctave, null)
            );
        }
    }

    private void shiftOctaveUp() {
        if (baseOctave < 6) {
            baseOctave++;
            recreatePiano();
            logger.info("Piano base octave changed to: {} (via keyboard)", baseOctave);

            // Show visual feedback - update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Octave: " + baseOctave, null)
            );
        }
    }

    private void setupControlButtons() {
        int buttonWidth = 25;
        int buttonHeight = 15;
        int startX = getPreferredSize().width - buttonWidth - 5;
        int startY = 12;
        int spacing = 5;

        followScaleBtn = new JButton();
        followScaleBtn.setBounds(startX, startY, buttonWidth, buttonHeight);
        followScaleBtn.setBackground(UIHelper.coolBlue);
        followScaleBtn.setToolTipText("Follow current scale");
        configureToggleButton(followScaleBtn);

        JButton octaveDownBtn = new JButton();
        octaveDownBtn.setBounds(startX, startY + buttonHeight + spacing, buttonWidth, buttonHeight);
        octaveDownBtn.setBackground(UIHelper.warmMustard);
        octaveDownBtn.setToolTipText("Octave down (Left Arrow)");
        configureToggleButton(octaveDownBtn);

        octaveDownBtn.addActionListener(e -> shiftOctaveDown());

        JButton octaveUpBtn = new JButton();
        octaveUpBtn.setBounds(startX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight);
        octaveUpBtn.setBackground(UIHelper.fadedOrange);
        octaveUpBtn.setToolTipText("Octave up (Right Arrow)");
        configureToggleButton(octaveUpBtn);

        octaveUpBtn.addActionListener(e -> shiftOctaveUp());

        add(followScaleBtn);
        add(octaveDownBtn);
        add(octaveUpBtn);
    }

    private void recreatePiano() {
        // Remove all existing keys
        for (JButton key : noteToKeyMap.values()) {
            remove(key);
        }
        noteToKeyMap.clear();

        // Recreate keys with new octave range
        createMultiOctavePiano();
        revalidate();
        repaint();
    }

    private void createMultiOctavePiano() {
        // Dimensions for keys
        int whiteKeyWidth = 30;
        int whiteKeyHeight = 80;
        int blackKeyWidth = 18;
        int blackKeyHeight = 50;

        // Notes for creating keys
        String[] whiteNotes = {"C", "D", "E", "F", "G", "A", "B"};
        String[] blackNotes = {"C#", "D#", "", "F#", "G#", "A#", ""};

        // Create keys for each octave
        for (int octave = 0; octave < OCTAVE_COUNT; octave++) {
            int octaveOffset = octave * 7 * whiteKeyWidth; // X offset for this octave
            int midiOctave = baseOctave + octave; // MIDI octave number

            // Create white keys for this octave
            for (int i = 0; i < 7; i++) {
                String noteName = whiteNotes[i] + midiOctave;
                JButton whiteKey = createPianoKey(true, noteName);
                whiteKey.setBounds(i * whiteKeyWidth + 10 + octaveOffset, 10, whiteKeyWidth - 1, whiteKeyHeight);
                add(whiteKey);

                // Map MIDI note to key
                int midiNote = (midiOctave * 12) + getMidiOffsetForWhiteKey(i);
                noteToKeyMap.put(midiNote, whiteKey);
            }

            // Create black keys for this octave
            for (int i = 0; i < 7; i++) {
                if (!blackNotes[i].isEmpty()) {
                    String noteName = blackNotes[i] + midiOctave;
                    JButton blackKey = createPianoKey(false, noteName);
                    blackKey.setBounds(i * whiteKeyWidth + whiteKeyWidth / 2 + 10 + octaveOffset,
                            10, blackKeyWidth, blackKeyHeight);
                    add(blackKey, 0); // Add black keys first so they appear on top

                    // Map MIDI note to key
                    int midiNote = (midiOctave * 12) + getMidiOffsetForBlackKey(i);
                    noteToKeyMap.put(midiNote, blackKey);
                }
            }
        }
    }

    private int getMidiOffsetForWhiteKey(int index) {
        // Maps key index to semitone offset within octave
        int[] offsets = {0, 2, 4, 5, 7, 9, 11}; // C, D, E, F, G, A, B
        return offsets[index];
    }

    private int getMidiOffsetForBlackKey(int index) {
        // Maps key index to semitone offset within octave
        int[] offsets = {1, 3, -1, 6, 8, 10, -1}; // C#, D#, (none), F#, G#, A#, (none)
        return offsets[index];
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Create gradient background using animated color
        Color currentColor = colorAnimator.getCurrentColor();
        Color darkerColor = darker(currentColor, 0.7f);

        GradientPaint gradient = new GradientPaint(
                0, 0, currentColor,
                0, getHeight(), darkerColor);

        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.dispose();
    }

    private Color darker(Color color, float factor) {
        return new Color(
                Math.max((int) (color.getRed() * factor), 0),
                Math.max((int) (color.getGreen() * factor), 0),
                Math.max((int) (color.getBlue() * factor), 0));
    }

    private void setupActionBusListener() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.KEY_PRESSED -> {
                        if (action.getData() instanceof Integer note) {
                            handleKeyPress(note);
                        }
                    }
                    case Commands.KEY_HELD -> {
                        if (action.getData() instanceof Integer note) {
                            handleKeyHold(note);
                        }
                    }
                    case Commands.KEY_RELEASED -> {
                        if (action.getData() instanceof Integer note) {
                            handleKeyRelease(note);
                        }
                    }
                    case Commands.SCALE_SELECTED -> {
                        if (activeButton == followScaleBtn && action.getData() instanceof String scaleName) {
                            currentScale = scaleName;
                            applyCurrentScale();
                        }
                    }
                    case Commands.ROOT_NOTE_SELECTED -> {
                        if (action.getData() instanceof String rootNote) {
                            currentRoot = rootNote;
                            applyCurrentScale();
                        }
                    }
                }
            }
        }, new String[] {
                Commands.KEY_PRESSED,
                Commands.KEY_HELD,
                Commands.KEY_RELEASED,
                Commands.SCALE_SELECTED,
                Commands.ROOT_NOTE_SELECTED
            });
    }

    private void handleKeyPress(int note) {
        if (!heldNotes.contains(note)) {
            // Update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Playing", "Playing note " + note)
            );

            // Visual feedback
            highlightKey(note);

            // Send MIDI note to internal synth
            playNote(note);

            // If not held, schedule release
            if (!heldNotes.contains(note)) {
                releaseNoteAfterDelay(note);
            }
        }
    }

    private void handleKeyHold(int note) {
        // Toggle behavior: if note is already held, release it
        if (heldNotes.contains(note)) {
            heldNotes.remove(note);
            unhighlightKey(note);
            CommandBus.getInstance().publish(Commands.CLEAR_STATUS, this, null);
        } else {
            // Add to held notes and play
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Holding note " + note, null)
            );

            heldNotes.add(note);
            highlightKey(note);
            playNote(note);
        }
    }

    private void handleKeyRelease(int note) {
        if (!heldNotes.contains(note)) {
            unhighlightKey(note);
            stopNote(note);
            CommandBus.getInstance().publish(Commands.CLEAR_STATUS, this, null);
        }
    }

    private void playNote(int note) {
        // Play through internal synth
        MidiService.getInstance().playNote(note, 100, 500, SYNTH_MIDI_CHANNEL);

        // Update status
        CommandBus.getInstance().publish(
                Commands.STATUS_UPDATE,
                this,
                new StatusUpdate(getClass().getSimpleName(), "Playing note " + note + " on Internal Synth", null)
        );
    }

    private void stopNote(int note) {
        // Note-off is handled automatically by the InternalSynthManager
        CommandBus.getInstance().publish(Commands.CLEAR_STATUS, this, null);
    }

    private void highlightKey(int note) {
        SwingUtilities.invokeLater(() -> {
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                key.getModel().setPressed(true);
                key.getModel().setArmed(true);
            }
        });
    }

    private void unhighlightKey(int note) {
        SwingUtilities.invokeLater(() -> {
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                key.getModel().setPressed(false);
                key.getModel().setArmed(false);
            }
        });
    }

    private void releaseNoteAfterDelay(int note) {
        new Timer(150, e -> {
            if (!heldNotes.contains(note)) {
                handleKeyRelease(note);
            }
            ((Timer) e.getSource()).stop();
        }).start();
    }

    private JButton createPianoKey(boolean isWhite, String note) {
        JButton key = new JButton();
        key.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                boolean isPressed = ((JButton) c).getModel().isPressed();
                boolean isHeld = heldNotes.contains(getNoteForKey((JButton) c));

                // Traditional colors with better contrast
                if (isWhite) {
                    // White keys
                    if (isPressed || isHeld) {
                        g2d.setColor(new Color(180, 180, 180)); // Darker when pressed
                    } else {
                        g2d.setColor(new Color(248, 248, 248)); // Slightly off-white
                    }
                    g2d.fillRect(0, isPressed ? 2 : 0, w, h);

                    if (!isPressed && !isHeld) {
                        // Top shadow for 3D effect
                        g2d.setColor(new Color(255, 255, 255));
                        g2d.fillRect(0, 0, w, 5);
                    }
                } else {
                    // Black keys
                    if (isPressed || isHeld) {
                        g2d.setColor(new Color(100, 100, 100)); // Lighter when pressed
                    } else {
                        g2d.setColor(new Color(40, 40, 40)); // Dark but not pure black
                    }
                    g2d.fillRect(0, 0, w, h);

                    // Add subtle gradient for better visibility
                    if (!isPressed && !isHeld) {
                        for (int i = 0; i < 10; i++) {
                            int alpha = 15 + (i * 5);
                            g2d.setColor(new Color(255, 255, 255, alpha));
                            g2d.fillRect(0, h - 10 + i, w, 1);
                        }
                    }
                }

                // Border colors
                g2d.setColor(isWhite ? new Color(180, 180, 180) : new Color(0, 0, 0));
                g2d.drawRect(0, 0, w - 1, h - 1);

                // Draw note labels on white keys
                if (isWhite) {
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    FontMetrics fm = g2d.getFontMetrics();

                    // Extract just the note name without octave
                    String noteText = note.length() > 1 ? note.substring(0, 1) : note;
                    int noteWidth = fm.stringWidth(noteText);
                    g2d.drawString(noteText, (w - noteWidth) / 2, h - 15);

                    // Draw octave number smaller
                    if (note.length() > 1) {
                        String octaveText = note.substring(1);
                        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                        g2d.drawString(octaveText, (w - noteWidth) / 2 + noteWidth, h - 15);
                    }
                }

                g2d.dispose();
            }
        });

        // Setup key properties
        key.setPressedIcon(null);
        key.setContentAreaFilled(false);
        key.setBorderPainted(false);
        key.setFocusPainted(false);
        key.setToolTipText(note);

        // Add mouse listeners
        key.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                Integer noteValue = getNoteForKey(key);
                if (noteValue != null) {
                    if (e.isShiftDown()) {
                        // Shift+click for hold
                        handleKeyHold(noteValue);
                    } else {
                        // Regular click for play
                        handleKeyPress(noteValue);
                    }
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                Integer noteValue = getNoteForKey(key);
                if (noteValue != null && !heldNotes.contains(noteValue)) {
                    // Only release if not in held notes list
                    handleKeyRelease(noteValue);
                }
            }
        });

        return key;
    }

    private Integer getNoteForKey(JButton key) {
        for (Map.Entry<Integer, JButton> entry : noteToKeyMap.entrySet()) {
            if (entry.getValue() == key) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void configureToggleButton(JButton button) {
        Color defaultColor = button.getBackground();
        button.addActionListener(e -> {
            if (activeButton == button) {
                // Deactivate if clicking the active button
                button.setBackground(defaultColor);
                activeButton = null;

                // Release all held scale notes when deactivating first button
                if (button == followScaleBtn)
                    releaseAllNotes();

            } else {
                // Restore previous active button's color
                if (activeButton != null) {
                    activeButton.setBackground((Color) activeButton.getClientProperty("defaultColor"));
                }
                // Activate new button
                button.putClientProperty("defaultColor", defaultColor);
                button.setBackground(Color.GREEN);
                activeButton = button;

                // If it's the follow scale button, apply current scale
                if (button == followScaleBtn)
                    applyCurrentScale();
            }
        });
        button.putClientProperty("defaultColor", defaultColor);
    }

    private void applyCurrentScale() {
        // First release any previously held notes
        releaseAllNotes();

        // Get the scale pattern based on current root and scale type
        Boolean[] scaleNotes = Scale.getScale(currentRoot, currentScale);

        // Collect the notes that are part of the scale
        List<Integer> scaleNotesList = new ArrayList<>();

        // Apply scale across all octaves
        for (int octave = baseOctave; octave < baseOctave + OCTAVE_COUNT; octave++) {
            // Calculate base note for this octave
            int rootOffset = Scale.getRootOffset(currentRoot);
            int baseNote = octave * 12 + rootOffset;

            // Map scale positions to MIDI notes
            for (int i = 0; i < scaleNotes.length; i++) {
                if (scaleNotes[i]) {
                    int midiNote = baseNote + i;
                    JButton key = noteToKeyMap.get(midiNote);
                    if (key != null) {
                        // Mark as held and highlight
                        heldNotes.add(midiNote);
                        highlightKey(midiNote);
                        scaleNotesList.add(midiNote);
                    }
                }
            }
        }

        // Play the notes as a chord if the scale button is active
        if (activeButton == followScaleBtn && !scaleNotesList.isEmpty()) {
            // Update status
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(
                            getClass().getSimpleName(),
                            String.format("Playing %s %s scale", currentRoot, currentScale),
                            null
                    )
            );

            // Play each note with slight delay for arpeggio effect
            final int[] delayMs = {0};
            Timer chordTimer = new Timer(30, null);
            chordTimer.addActionListener(e -> {
                if (delayMs[0] < scaleNotesList.size()) {
                    int noteToPlay = scaleNotesList.get(delayMs[0]);
                    playNote(noteToPlay);
                    delayMs[0]++;
                } else {
                    ((Timer) e.getSource()).stop();
                }
            });
            chordTimer.start();
        }
    }

    private void releaseAllNotes() {
        new HashSet<>(heldNotes).forEach(note -> {
            heldNotes.remove(note);
            unhighlightKey(note);
        });
    }

    private void setupPlayerStatusIndicator() {
        playerStatusIndicator = new JLabel("●");
        playerStatusIndicator.setFont(new Font("Arial", Font.BOLD, 12));
        playerStatusIndicator.setForeground(Color.GREEN); // Always green for internal synth
        playerStatusIndicator.setBounds(getWidth() - 15, getHeight() - 15, 10, 10);
        playerStatusIndicator.setToolTipText("Internal Synthesizer");
        add(playerStatusIndicator);
    }
}
