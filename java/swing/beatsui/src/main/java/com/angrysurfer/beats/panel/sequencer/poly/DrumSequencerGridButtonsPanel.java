package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.DrumSequencerGridPanelContextHandler;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.DrumStepParametersEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.DrumSequencerManager;

/**
 * Custom-painted drum sequencer grid. Replaces the previous JButton-per-cell
 * approach with a single paintComponent call, eliminating per-button repaint
 * scheduling and the flicker that came with it.
 */
public class DrumSequencerGridButtonsPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerGridButtonsPanel.class);

    private static final int DRUM_PAD_COUNT = SequencerConstants.DRUM_PAD_COUNT;
    private static final int GAP = 2;

    // Cached colors
    private static final Color COLOR_ACTIVE    = new Color(60, 180, 120);
    private static final Color COLOR_INACTIVE  = new Color(60, 60, 60);
    private static final Color COLOR_DISABLED  = new Color(40, 40, 40);
    private static final Color COLOR_ACCENT    = new Color(220, 180, 60);

    private final DrumSequencer sequencer;
    private final DrumSequencerGridPanel parentPanel;
    private final DrumSequencerGridPanelContextHandler contextMenuHandler;

    // Per-cell state arrays  [drum][step]
    private final boolean[][] active;
    private final boolean[][] inPattern;
    private final boolean[][] highlighted;
    private final boolean[][] accented;
    private final Color[][]   highlightColor;
    private final int[][]     velocity;
    private final int[][]     rowBorderStyle; // 0=normal, 1=selected

    // Playback state
    private volatile boolean isPlaying = false;

    // The canvas that does the actual painting
    private final Canvas canvas;

    public DrumSequencerGridButtonsPanel(DrumSequencer sequencer, DrumSequencerGridPanel parentPanel) {
        super(new BorderLayout());
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;
        this.contextMenuHandler = new DrumSequencerGridPanelContextHandler(sequencer, parentPanel);

        int cols = sequencer.getDefaultPatternLength();
        active       = new boolean[DRUM_PAD_COUNT][cols];
        inPattern    = new boolean[DRUM_PAD_COUNT][cols];
        highlighted  = new boolean[DRUM_PAD_COUNT][cols];
        accented     = new boolean[DRUM_PAD_COUNT][cols];
        highlightColor = new Color[DRUM_PAD_COUNT][cols];
        velocity     = new int[DRUM_PAD_COUNT][cols];
        rowBorderStyle = new int[DRUM_PAD_COUNT][cols];

        // Default velocity
        for (int[] row : velocity) Arrays.fill(row, 100);

        // Initialise state from sequencer
        syncAllFromSequencer();

        canvas = new Canvas();
        add(canvas, BorderLayout.CENTER);

        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                Commands.DRUM_STEP_EFFECTS_CHANGED,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP,
                Commands.DRUM_SEQUENCE_LOADED,
                Commands.DRUM_SEQUENCE_UPDATED,
                Commands.DRUM_GRID_REFRESH_REQUESTED
        });
    }

    // -------------------------------------------------------------------------
    // Canvas — single paintComponent for the entire grid
    // -------------------------------------------------------------------------

    private class Canvas extends JPanel {

        Canvas() {
            setBackground(Color.DARK_GRAY);
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { handleMouse(e, false); }
                @Override public void mouseReleased(MouseEvent e) { handleMouse(e, true);  }
            };
            addMouseListener(ma);
        }

        private void handleMouse(MouseEvent e, boolean released) {
            int[] rc = toRowCol(e.getX(), e.getY());
            if (rc == null) return;
            int drum = rc[0], step = rc[1];

            if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                contextMenuHandler.showContextMenu(this, e.getX(), e.getY(), drum, step);
                return;
            }
            if (!released && e.getButton() == MouseEvent.BUTTON1) {
                // Toggle step
                sequencer.toggleStep(drum, step);
                active[drum][step] = sequencer.isStepActive(drum, step);
                accented[drum][step] = sequencer.isStepAccented(drum, step);
                velocity[drum][step] = sequencer.getStepVelocity(drum, step);
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            Insets ins = getInsets();
            int cols = sequencer.getDefaultPatternLength();
            int availW = getWidth()  - ins.left - ins.right;
            int availH = getHeight() - ins.top  - ins.bottom;
            int cellW = (availW - (cols - 1)           * GAP) / cols;
            int cellH = (availH - (DRUM_PAD_COUNT - 1) * GAP) / DRUM_PAD_COUNT;
            if (cellW < 1 || cellH < 1) return;

            for (int drum = 0; drum < DRUM_PAD_COUNT; drum++) {
                for (int step = 0; step < cols; step++) {
                    int x = ins.left + step * (cellW + GAP);
                    int y = ins.top  + drum * (cellH + GAP);

                    // Base fill
                    if (highlighted[drum][step]) {
                        Color hc = highlightColor[drum][step];
                        g2.setColor(hc != null ? hc : UIHelper.fadedOrange);
                    } else if (!inPattern[drum][step]) {
                        g2.setColor(COLOR_DISABLED);
                    } else if (active[drum][step]) {
                        g2.setColor(COLOR_ACTIVE);
                    } else {
                        g2.setColor(COLOR_INACTIVE);
                    }
                    g2.fillRect(x, y, cellW, cellH);

                    // Velocity bar (bottom strip)
                    if (active[drum][step] && inPattern[drum][step] && !highlighted[drum][step]) {
                        int barH = Math.max(1, (int)(cellH * (velocity[drum][step] / 127.0) * 0.25));
                        g2.setColor(new Color(255, 255, 0, 120));
                        g2.fillRect(x, y + cellH - barH, cellW / 4, barH);
                    }

                    // Accent dot
                    if (accented[drum][step] && inPattern[drum][step]) {
                        int sq = Math.max(3, Math.min(cellW, cellH) / 5);
                        g2.setColor(UIHelper.agedOffWhite);
                        g2.fillRect(x, y, sq, sq);
                    }

                    // Selected-row border
                    if (rowBorderStyle[drum][step] == 1) {
                        g2.setColor(UIHelper.dustyAmber);
                        g2.drawRect(x, y, cellW - 1, cellH - 1);
                    }
                }
            }
        }

        private int[] toRowCol(int px, int py) {
            Insets ins = getInsets();
            int cols = sequencer.getDefaultPatternLength();
            int availW = getWidth()  - ins.left - ins.right;
            int availH = getHeight() - ins.top  - ins.bottom;
            int cellW = (availW - (cols - 1)           * GAP) / cols;
            int cellH = (availH - (DRUM_PAD_COUNT - 1) * GAP) / DRUM_PAD_COUNT;
            if (cellW < 1 || cellH < 1) return null;
            int col = (px - ins.left) / (cellW + GAP);
            int row = (py - ins.top)  / (cellH + GAP);
            if (row >= 0 && row < DRUM_PAD_COUNT && col >= 0 && col < cols) return new int[]{row, col};
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Public API (same contract as before)
    // -------------------------------------------------------------------------

    /** Called from the sequencer callback — may arrive on any thread. */
    public void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) return;
        int cols = sequencer.getDefaultPatternLength();

        // Update state arrays (safe to write from timing thread — reads happen on EDT in paintComponent)
        if (oldStep >= 0 && oldStep < cols) highlighted[drumIndex][oldStep] = false;
        if (isPlaying && newStep >= 0 && newStep < cols) {
            highlighted[drumIndex][newStep] = true;
            highlightColor[drumIndex][newStep] = stepHighlightColor(newStep);
        }

        // One repaint call — Swing's repaint manager coalesces these automatically
        canvas.repaint();
    }

    public void updateBackwardStepHighlighting(int drumIndex, int oldStep, int newStep) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) return;
        int cols = sequencer.getDefaultPatternLength();

        // Clear entire row then set new step
        Arrays.fill(highlighted[drumIndex], false);
        if (isPlaying && newStep >= 0 && newStep < cols) {
            highlighted[drumIndex][newStep] = true;
            highlightColor[drumIndex][newStep] = stepHighlightColor(newStep);
        }
        canvas.repaint();
    }

    public void updateRowAppearance(int drumIndex, boolean isSelected) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) return;
        int cols = sequencer.getDefaultPatternLength();
        int patternLength = sequencer.getPatternLength(drumIndex);
        for (int step = 0; step < cols; step++) {
            inPattern[drumIndex][step] = step < patternLength;
            active[drumIndex][step]    = inPattern[drumIndex][step] && sequencer.isStepActive(drumIndex, step);
            rowBorderStyle[drumIndex][step] = isSelected ? 1 : 0;
        }
        canvas.repaint();
    }

    public void updateStepButtonsForDrum(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT) return;
        int cols = sequencer.getDefaultPatternLength();
        int patternLength = sequencer.getPatternLength(drumIndex);
        for (int step = 0; step < cols; step++) {
            inPattern[drumIndex][step] = step < patternLength;
            active[drumIndex][step]    = inPattern[drumIndex][step] && sequencer.isStepActive(drumIndex, step);
            accented[drumIndex][step]  = sequencer.isStepAccented(drumIndex, step);
            velocity[drumIndex][step]  = sequencer.getStepVelocity(drumIndex, step);
        }
        SwingUtilities.invokeLater(canvas::repaint);
    }

    public void clearAllStepHighlighting() {
        for (boolean[] row : highlighted) Arrays.fill(row, false);
        canvas.repaint();
    }

    public void setPlayingState(boolean playing) {
        this.isPlaying = playing;
        if (!playing) clearAllStepHighlighting();
    }

    public void refreshGridUI() {
        SwingUtilities.invokeLater(() -> {
            syncAllFromSequencer();
            canvas.revalidate();
            canvas.repaint();
        });
    }

    public void toggleDebugMode() {
        // no-op in canvas mode — debug info can be added to paintComponent if needed
    }

    // -------------------------------------------------------------------------
    // IBusListener
    // -------------------------------------------------------------------------

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;
        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_UPDATED ->
                SwingUtilities.invokeLater(this::refreshGridUI);

            case Commands.DRUM_STEP_PARAMETERS_CHANGED -> {
                if (action.getData() instanceof DrumStepParametersEvent ev) {
                    int d = ev.getDrumIndex(), s = ev.getStepIndex();
                    if (d >= 0 && d < DRUM_PAD_COUNT && s >= 0 && s < sequencer.getDefaultPatternLength()) {
                        active[d][s]   = ev.isActive();
                        accented[d][s] = ev.isAccented();
                        velocity[d][s] = ev.getVelocity();
                        SwingUtilities.invokeLater(canvas::repaint);
                    }
                }
            }

            case Commands.DRUM_STEP_EFFECTS_CHANGED -> {
                // Effects (pan/chorus/reverb) not currently visualised — repaint anyway
                SwingUtilities.invokeLater(canvas::repaint);
            }

            case Commands.TRANSPORT_START  -> setPlayingState(true);
            case Commands.TRANSPORT_STOP   -> setPlayingState(false);
            case Commands.DRUM_GRID_REFRESH_REQUESTED -> refreshGridUI();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void syncAllFromSequencer() {
        int cols = sequencer.getDefaultPatternLength();
        int selectedPad = DrumSequencerManager.getInstance().getSelectedPadIndex();
        for (int drum = 0; drum < DRUM_PAD_COUNT; drum++) {
            int patternLength = sequencer.getPatternLength(drum);
            for (int step = 0; step < cols; step++) {
                inPattern[drum][step] = step < patternLength;
                active[drum][step]    = inPattern[drum][step] && sequencer.isStepActive(drum, step);
                accented[drum][step]  = sequencer.isStepAccented(drum, step);
                velocity[drum][step]  = sequencer.getStepVelocity(drum, step);
                highlighted[drum][step] = false;
                rowBorderStyle[drum][step] = (drum == selectedPad) ? 1 : 0;
            }
        }
    }

    private static Color stepHighlightColor(int step) {
        if (step < 16) return UIHelper.fadedOrange;
        if (step < 32) return UIHelper.coolBlue;
        if (step < 48) return UIHelper.deepNavy;
        return UIHelper.mutedOlive;
    }
}
