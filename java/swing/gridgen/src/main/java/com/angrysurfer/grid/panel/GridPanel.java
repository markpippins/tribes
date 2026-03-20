package com.angrysurfer.grid.panel;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.angrysurfer.grid.visualization.Visualizer;
import com.angrysurfer.grid.widget.GridButton;


public class GridPanel extends JPanel {

    private GridButton[][] buttons;
    private Visualizer gridSaver;

    static int GRID_ROWS = 32;
    static int GRID_COLS = 64;

    public GridPanel() {
        super(new GridLayout(GRID_ROWS, GRID_COLS, 2, 2));
        setup();
        gridSaver = new Visualizer(this, buttons);
    }

    private void setup() {
        // Reduce horizontal spacing in GridLayout to 1 pixel
        setLayout(new GridLayout(GRID_ROWS, GRID_COLS, 1, 1));
        
        // Reduce border padding, especially on left and right
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        buttons = new GridButton[GRID_ROWS][GRID_COLS];

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gridSaver.isVisualizationMode()) {
                    gridSaver.stopVisualizer();
                }
            }
        };

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                buttons[row][col] = new GridButton(row, col);
                buttons[row][col].addMouseListener(mouseHandler);
                add(buttons[row][col]);
            }
        }
    }
}
