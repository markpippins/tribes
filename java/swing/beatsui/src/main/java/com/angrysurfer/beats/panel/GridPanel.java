package com.angrysurfer.beats.panel;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.angrysurfer.beats.widget.GridButton;


public class GridPanel extends JPanel {

    private GridButton[][] buttons;

    static int GRID_ROWS = 32;
    static int GRID_COLS = 64;

    public GridPanel() {
        super(new GridLayout(GRID_ROWS, GRID_COLS, 1, 1));
        setup();
    }

    private void setup() {
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        buttons = new GridButton[GRID_ROWS][GRID_COLS];

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col] = new GridButton(row, col);
                add(buttons[row][col]);
            }
        }
    }
}
