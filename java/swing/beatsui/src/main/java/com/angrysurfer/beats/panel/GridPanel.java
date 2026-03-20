package com.angrysurfer.beats.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Lightweight grid panel that renders all cells in a single paintComponent call.
 * Replaces the previous 2048-JButton implementation.
 */
class GridPanel extends JPanel {

    static int GRID_ROWS = 32;
    static int GRID_COLS = 64;

    private static final int GAP = 1;

    private final Color[][] cellColors = new Color[GRID_ROWS][GRID_COLS];
    private final String[][] cellLabels = new String[GRID_ROWS][GRID_COLS];

    public GridPanel() {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setBackground(Color.DARK_GRAY);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int[] rc = toRowCol(e.getX(), e.getY());
                if (rc != null) onCellClicked(rc[0], rc[1], e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int[] rc = toRowCol(e.getX(), e.getY());
                if (rc != null) onCellDragged(rc[0], rc[1], e);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    // --- public API ---

    public void setCell(int row, int col, Color color) {
        cellColors[row][col] = color;
        repaintCell(row, col);
    }

    public void setCell(int row, int col, Color color, String label) {
        cellColors[row][col] = color;
        cellLabels[row][col] = label;
        repaintCell(row, col);
    }

    public void clearCell(int row, int col) {
        cellColors[row][col] = null;
        cellLabels[row][col] = null;
        repaintCell(row, col);
    }

    public void clearAll() {
        for (int r = 0; r < GRID_ROWS; r++)
            for (int c = 0; c < GRID_COLS; c++) {
                cellColors[r][c] = null;
                cellLabels[r][c] = null;
            }
        repaint();
    }

    // --- override for interaction ---

    protected void onCellClicked(int row, int col, MouseEvent e) {}
    protected void onCellDragged(int row, int col, MouseEvent e) {}

    // --- painting ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Insets ins = getInsets();
        int availW = getWidth() - ins.left - ins.right;
        int availH = getHeight() - ins.top - ins.bottom;
        int cellW = (availW - (GRID_COLS - 1) * GAP) / GRID_COLS;
        int cellH = (availH - (GRID_ROWS - 1) * GAP) / GRID_ROWS;

        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                int x = ins.left + c * (cellW + GAP);
                int y = ins.top + r * (cellH + GAP);
                Color col = cellColors[r][c];
                g.setColor(col != null ? col : Color.DARK_GRAY);
                g.fillRect(x, y, cellW, cellH);
                String lbl = cellLabels[r][c];
                if (lbl != null && !lbl.isEmpty()) {
                    g.setColor(Color.WHITE);
                    g.setFont(g.getFont().deriveFont(8f));
                    g.drawString(lbl, x + 1, y + cellH - 2);
                }
            }
        }
    }

    // --- helpers ---

    private void repaintCell(int row, int col) {
        Insets ins = getInsets();
        int availW = getWidth() - ins.left - ins.right;
        int availH = getHeight() - ins.top - ins.bottom;
        int cellW = (availW - (GRID_COLS - 1) * GAP) / GRID_COLS;
        int cellH = (availH - (GRID_ROWS - 1) * GAP) / GRID_ROWS;
        int x = ins.left + col * (cellW + GAP);
        int y = ins.top + row * (cellH + GAP);
        repaint(x, y, cellW, cellH);
    }

    private int[] toRowCol(int px, int py) {
        Insets ins = getInsets();
        int availW = getWidth() - ins.left - ins.right;
        int availH = getHeight() - ins.top - ins.bottom;
        int cellW = (availW - (GRID_COLS - 1) * GAP) / GRID_COLS;
        int cellH = (availH - (GRID_ROWS - 1) * GAP) / GRID_ROWS;
        int col = (px - ins.left) / (cellW + GAP);
        int row = (py - ins.top) / (cellH + GAP);
        if (row >= 0 && row < GRID_ROWS && col >= 0 && col < GRID_COLS) return new int[]{row, col};
        return null;
    }
}
