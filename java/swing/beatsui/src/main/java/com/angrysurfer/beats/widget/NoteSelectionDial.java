package com.angrysurfer.beats.widget;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.Scale;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

@Getter
@Setter
public class NoteSelectionDial extends Dial implements IBusListener {

    static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final Logger logger = LoggerFactory.getLogger(NoteSelectionDial.class);
    private static final int DETENT_COUNT = 12;
    private static final double SNAP_THRESHOLD = 0.2;
    private static final double START_ANGLE = -90; // Start at top (-90 degrees)
    private static final double TOTAL_ARC = 360; // Full circle
    private static final int NOTES_PER_OCTAVE = 12;
    private static final double DEGREES_PER_DETENT = 360.0 / DETENT_COUNT;

    private int currentDetent = 0; // Note within octave (0-11)
    private int midiNote = 60; // Full MIDI note (0-127)
    private int octave = 4; // Current octave (default to middle C = C4)

    private boolean isDragging = false;
    private double startAngle = 0;
    private boolean infiniteTurn = true;

    private int rootNoteIndex = 0; // Default to C (index 0)
    private String[] rotatedNoteNames; // Will hold rotated note names based on root

    private boolean followGlobal = true;

    private Integer sequencerId;

    public NoteSelectionDial() {
        super();
        setMinimum(0);
        setMaximum(127); // Full MIDI range
        setValue(60, false); // Default to middle C (MIDI note 60)

        setPreferredSize(new Dimension(100, 100));
        setMinimumSize(new Dimension(100, 100));

        // Initialize rotated note names
        rotatedNoteNames = NOTE_NAMES.clone();

        // Register for root note events
        CommandBus.getInstance().register(this, new String[]{
                Commands.ROOT_NOTE_SELECTED,
                Commands.SEQUENCER_ROOT_NOTE_SELECTED
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!isEnabled())
                    return;
                isDragging = true;
                Point center = new Point(getWidth() / 2, getHeight() / 2);
                startAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);

                // Show note selection popup
                if (e.getClickCount() == 2) {
                    showNoteSelectionPopup(e);
                }
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (!isEnabled() || !isDragging)
                    return;

                Point center = new Point(getWidth() / 2, getHeight() / 2);
                double currentAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);

                double angleDelta = currentAngle - startAngle;
                if (angleDelta > Math.PI)
                    angleDelta -= 2 * Math.PI;
                if (angleDelta < -Math.PI)
                    angleDelta += 2 * Math.PI;

                double angleDegrees = Math.toDegrees(angleDelta);
                int detentDelta = (int) Math.round(angleDegrees / DEGREES_PER_DETENT);

                if (detentDelta != 0) {
                    // Calculate new note position (0-11) within the octave
                    int newDetent = (currentDetent + detentDelta) % NOTES_PER_OCTAVE;
                    if (newDetent < 0)
                        newDetent += NOTES_PER_OCTAVE;

                    // Only update if the note changes
                    if (newDetent != currentDetent) {
                        currentDetent = newDetent;

                        // Calculate the actual note index considering root rotation
                        int actualNoteIndex = (currentDetent + rootNoteIndex) % NOTES_PER_OCTAVE;

                        // Calculate new MIDI note preserving octave
                        int newMidiNote = Scale.getMidiNote(NOTE_NAMES[actualNoteIndex], octave);

                        // Store old value for change detection
                        int oldValue = midiNote;
                        midiNote = newMidiNote;

                        // Update the visual representation
                        startAngle = currentAngle;
                        repaint();

                        // Fire change events if value changed
                        if (oldValue != newMidiNote) {
                            NoteSelectionDial.this.setValue(newMidiNote, true);
                            logger.debug("Note changed: {} (MIDI {}, detent {})",
                                    NOTE_NAMES[actualNoteIndex] + octave, newMidiNote, currentDetent);
                        }
                    }
                }
            }
        });
    }

    // Method for handling double-click to select note
    private void showNoteSelectionPopup(MouseEvent e) {
        // Implementation for a note selection popup
        // (This can be implemented later if desired)
    }


    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 20;
        int margin = size / 8;

        // Center coordinates
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radius = (size - 2 * margin) / 2.0;

        // Draw dial background
        g2d.setColor(getParent().getBackground());
        g2d.fillOval(x + margin, y + margin, size - 2 * margin, size - 2 * margin);

        // Draw outer ring
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x + margin, y + margin, size - 2 * margin, size - 2 * margin);

        // Draw detent markers and labels
        Font font = new Font("SansSerif", Font.PLAIN, size / 10);
        g2d.setFont(font);

        for (int i = 0; i < NOTES_PER_OCTAVE; i++) {
            double angle = Math.toRadians(START_ANGLE + (i * DEGREES_PER_DETENT));

            // Calculate marker points
            Point2D p1 = new Point2D.Double(centerX + Math.cos(angle) * (radius - margin),
                    centerY + Math.sin(angle) * (radius - margin));
            Point2D p2 = new Point2D.Double(centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius);

            // Highlight current note
            if (i == currentDetent) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2f));
            } else if (rotatedNoteNames[i].equals(NOTE_NAMES[0])) {
                // Subtle highlight for C note (musical reference)
                g2d.setColor(new Color(120, 120, 180));
                g2d.setStroke(new BasicStroke(1.5f));
            } else {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setStroke(new BasicStroke(1f));
            }

            // Draw marker line
            g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());

            // Draw note name - use rotatedNoteNames instead of NOTE_NAMES
            Point2D labelPos = new Point2D.Double(centerX + Math.cos(angle) * (radius + margin * 1.2),
                    centerY + Math.sin(angle) * (radius + margin * 1.2));

            FontMetrics fm = g2d.getFontMetrics();
            String label = rotatedNoteNames[i];
            int labelW = fm.stringWidth(label);
            int labelH = fm.getHeight();

            g2d.drawString(label, (int) (labelPos.getX() - labelW / 2),
                    (int) (labelPos.getY() + labelH / 4));
        }

        // Draw pointer
        double pointerAngle = Math.toRadians(START_ANGLE + (currentDetent * DEGREES_PER_DETENT));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(isEnabled() ? Color.RED : Color.GRAY);
        g2d.drawLine((int) centerX, (int) centerY, (int) (centerX + Math.cos(pointerAngle) * (radius - margin / 2)),
                (int) (centerY + Math.sin(pointerAngle) * (radius - margin / 2)));

        // Draw octave indicator in center
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, size / 6));
        String octaveText = String.valueOf(octave);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(octaveText, (int) (centerX - fm.stringWidth(octaveText) / 2),
                (int) (centerY + fm.getHeight() / 4));

        g2d.dispose();
    }

    @Override
    public int getValue() {
        return midiNote; // Return the full MIDI note value
    }

    public int getNoteInOctave() {
        return currentDetent;
    }

    public int getOctave() {
        return octave;
    }

    @Override
    public void setValue(int midiNoteValue, boolean notify) {
        // Ensure value is within MIDI range
        midiNoteValue = Math.max(0, Math.min(127, midiNoteValue));

        // Extract octave and note information
        octave = Scale.getOctave(midiNoteValue);
        int noteIndex = midiNoteValue % NOTES_PER_OCTAVE;

        // Calculate detent position considering root rotation
        currentDetent = (noteIndex - rootNoteIndex + NOTES_PER_OCTAVE) % NOTES_PER_OCTAVE;
        midiNote = midiNoteValue;

        // Set the base class value
        super.setValue(midiNoteValue, notify);

        repaint();
    }

    /**
     * Changes just the note without changing the octave
     */
    public void setNoteOnly(String noteName, boolean notify) {
        // Find the index of the note name
        int noteIndex = -1;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(noteName)) {
                noteIndex = i;
                break;
            }
        }

        if (noteIndex != -1) {
            // Calculate new MIDI note preserving octave
            int newMidiNote = Scale.getMidiNote(noteName, octave);
            setValue(newMidiNote, notify);
        }
    }

    /**
     * Changes just the octave without changing the note
     */
    public void setOctaveOnly(int newOctave, boolean notify) {
        // Ensure octave is in valid range (typically -1 to 9 for MIDI)
        newOctave = Math.max(-1, Math.min(9, newOctave));

        // Calculate new MIDI note with same note but new octave
        int newMidiNote = Scale.getMidiNote(NOTE_NAMES[currentDetent], newOctave);
        setValue(newMidiNote, notify);
    }

    /**
     * Gets the note name with octave (e.g., "C4")
     */
    public String getNoteWithOctave() {
        return NOTE_NAMES[currentDetent] + octave;
    }

    /**
     * Sets the root note for the dial, rotating the notes so the root appears at 12 o'clock
     *
     * @param rootNote The name of the root note (e.g. "C", "F#")
     * @param notify   Whether to fire change events
     */
    public void setRootNote(String rootNote, boolean notify) {
        // Find the index of the root note
        int newRootIndex = -1;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(rootNote)) {
                newRootIndex = i;
                break;
            }
        }

        if (newRootIndex != -1 && newRootIndex != rootNoteIndex) {
            // Get current MIDI note before changes
            int oldMidiNote = midiNote;

            // Update root note index
            rootNoteIndex = newRootIndex;

            // Rotate note names to place root at 12 o'clock
            rotateNoteNames();

            // Recalculate MIDI note based on current detent with new root
            updateMidiNoteForRoot();

            // Repaint with new layout
            repaint();

            // Fire change event if note changed
            if (notify && oldMidiNote != midiNote) {
                super.setValue(midiNote, true);
            }

            logger.debug("Root note changed to: {}, MIDI note now: {}", rootNote, midiNote);
        }
    }

    /**
     * Gets the current root note
     */
    public String getRootNote() {
        return NOTE_NAMES[rootNoteIndex];
    }

    /**
     * Rotates the note names array so root appears at 12 o'clock position
     */
    private void rotateNoteNames() {
        rotatedNoteNames = new String[NOTE_NAMES.length];

        // Rotate note names to put root at 12 o'clock (0 position)
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            int sourceIndex = (i + rootNoteIndex) % NOTE_NAMES.length;
            rotatedNoteNames[i] = NOTE_NAMES[sourceIndex];
        }
    }

    /**
     * Updates the MIDI note based on current detent position and root note
     */
    private void updateMidiNoteForRoot() {
        // Calculate actual note based on detent position and rotation
        int actualNoteIndex = (currentDetent + rootNoteIndex) % NOTES_PER_OCTAVE;

        // Update MIDI note while preserving octave
        midiNote = Scale.getMidiNote(NOTE_NAMES[actualNoteIndex], octave);
    }

    /**
     * Handle command bus events
     */
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        // Skip self-generated events
        if (action.getSender() == this) return;

        switch (action.getCommand()) {
            case Commands.ROOT_NOTE_SELECTED:
                // Handle global root note changes
                if (followGlobal && action.getData() instanceof String rootNote) {
                    setRootNote(rootNote, true);
                    logger.debug("Following global root note change to: {}", rootNote);
                }
                break;

            case Commands.SEQUENCER_ROOT_NOTE_SELECTED:
                // Handle sequencer-specific root note changes
                if (!followGlobal && action.getData() instanceof Object[] data && sequencerId != null) {
                    if (data.length == 2 && data[0] instanceof Integer targetSequencerId && data[1] instanceof String rootNote) {

                        // Only apply if targeting our sequencer
                        if (sequencerId.equals(targetSequencerId)) {
                            setRootNote(rootNote, true);
                            logger.debug("Applied sequencer-specific root note change to: {}", rootNote);
                        }
                    }
                }
                break;
        }
    }
}
