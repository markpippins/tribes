package com.angrysurfer.beats.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/**
 * A lightweight grid panel that replaces the previous 2048-JButton approach.
 * All cells are drawn in a single paintComponent() call — no per-cell
 * Swing components, no per-cell repaint cycles, no per-cell event listeners.
 *
 * Callers interact via:
 *   setCell(row, col, color)   — set a cell color and schedule a repaint
 *   setCell(row, col, color, label) — set color + short text label
 *   clearAll()                 — reset every cell to the default background
 *   getRows() / getCols()      — grid dimensions
 */
public class GridPanel extends JPanel {

    public static final int ROWS = 32;
    public static final int COLS = 64;

    private static final int GAP = 1;          // px gap between cells
    private static final int MIN_CELL = 8;     // minimum cell size in px

    private static final Color DEFAULT_BG = new Color(45, 45, 45);

    // Cell state — only Color and an optional short label per cell
    private final Color[][]  cellColors = new Color[ROWS][COLS];
    private final String[][] cellLabels = new String[ROWS][COLS];

    // Cached cell geometry — recalculated on resize
    private int cellW = MIN_CELL;
    private int cellH = MIN_CELL;

    public GridPanel() {
        setBackground(new Color(30, 30, 30));
        setOpaque(true);
        clearAll();
        setupMouse();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                recalcCellSize();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }

    /** Set a cell's color. Schedules a repaint. */
    public void setCell(int row, int col, Color color) {
        if (outOfBounds(row, col)) return;
        cellColors[row][col] = color;
        repaintCell(row, col);
    }

    /** Set a cell's color and a short text label (null to clear). */
    public void setCell(int row, int col, Color color, String label) {
        if (outOfBounds(row, col)) return;
        cellColors[row][col] = color;
        cellLabels[row][col] = label;
        repaintCell(row, col);
    }

    public Color getCell(int row, int col) {
        if (outOfBounds(row, col)) return DEFAULT_BG;
        return cellColors[row][col] != null ? cellColors[row][col] : DEFAULT_BG;
    }

    /** Reset all cells to the default background color. */
    public void clearAll() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                cellColors[r][c] = DEFAULT_BG;
                cellLabels[r][c] = null;
            }
        }
        repaint();
    }

    /** Clear a single cell. */
    public void clearCell(int row, int col) {
        setCell(row, col, DEFAULT_BG, null);
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Only enable text AA when labels are present — skip geometry AA for speed
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = c * (cellW + GAP);
                int y = r * (cellH + GAP);

                Color color = cellColors[r][c];
                g2.setColor(color != null ? color : DEFAULT_BG);
                g2.fillRect(x, y, cellW, cellH);

                String label = cellLabels[r][c];
                if (label != null && !label.isEmpty()) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont().deriveFont(9f));
                    g2.drawString(label, x + 2, y + cellH - 2);
                }
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(COLS * (MIN_CELL + GAP), ROWS * (MIN_CELL + GAP));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void recalcCellSize() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        cellW = Math.max(MIN_CELL, (w - GAP) / COLS - GAP);
        cellH = Math.max(MIN_CELL, (h - GAP) / ROWS - GAP);
    }

    /** Repaint only the dirty cell rectangle instead of the whole panel. */
    private void repaintCell(int row, int col) {
        int x = col * (cellW + GAP);
        int y = row * (cellH + GAP);
        repaint(x, y, cellW + GAP, cellH + GAP);
    }

    private boolean outOfBounds(int row, int col) {
        return row < 0 || row >= ROWS || col < 0 || col >= COLS;
    }

    private void setupMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int[] cell = pixelToCell(e.getX(), e.getY());
                if (cell != null) onCellClicked(cell[0], cell[1], e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int[] cell = pixelToCell(e.getX(), e.getY());
                if (cell != null) onCellDragged(cell[0], cell[1], e);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private int[] pixelToCell(int px, int py) {
        int stride = cellW + GAP;
        int strideH = cellH + GAP;
        int col = px / stride;
        int row = py / strideH;
        // Reject clicks that land in the gap
        if (px % stride >= cellW) return null;
        if (py % strideH >= cellH) return null;
        if (outOfBounds(row, col)) return null;
        return new int[]{row, col};
    }

    /** Override to handle cell clicks. Default: no-op. */
    protected void onCellClicked(int row, int col, MouseEvent e) {}

    /** Override to handle cell drags. Default: no-op. */
    protected void onCellDragged(int row, int col, MouseEvent e) {}
}
