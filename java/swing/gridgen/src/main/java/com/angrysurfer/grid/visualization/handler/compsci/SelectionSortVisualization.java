package com.angrysurfer.grid.visualization.handler.compsci;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class SelectionSortVisualization implements IVisualizationHandler {
    private int[] array;
    private int currentPos = 0;
    private int comparePos = 1;
    private int minPos = 0;
    private boolean isSorted = false;
    private final Random random = new Random();
    
    private static final Color CURRENT_POS_COLOR = Color.RED;
    private static final Color COMPARING_COLOR = Color.YELLOW;
    private static final Color MIN_POS_COLOR = Color.MAGENTA;
    private static final Color SORTED_COLOR = Color.GREEN;
    private static final Color DEFAULT_COLOR = Color.BLUE;

    @Override
    public void update(JButton[][] buttons) {
        if (array == null || array.length != buttons[0].length) {
            initializeArray(buttons[0].length);
        }

        // Perform one step of selection sort
        if (!isSorted) {
            if (comparePos < array.length) {
                if (array[comparePos] < array[minPos]) {
                    minPos = comparePos;
                }
                comparePos++;
            } else {
                // Swap elements if needed
                if (minPos != currentPos) {
                    int temp = array[currentPos];
                    array[currentPos] = array[minPos];
                    array[minPos] = temp;
                }
                currentPos++;
                if (currentPos >= array.length - 1) {
                    isSorted = true;
                } else {
                    minPos = currentPos;
                    comparePos = currentPos + 1;
                }
            }
        }

        // Visualize the current state
        for (int col = 0; col < buttons[0].length; col++) {
            int height = array[col];
            for (int row = 0; row < buttons.length; row++) {
                Color color = DEFAULT_COLOR;
                if (row >= buttons.length - height) {
                    if (col == currentPos) {
                        color = CURRENT_POS_COLOR;
                    } else if (col == comparePos) {
                        color = COMPARING_COLOR;
                    } else if (col == minPos) {
                        color = MIN_POS_COLOR;
                    } else if (col < currentPos) {
                        color = SORTED_COLOR;
                    }
                    buttons[row][col].setBackground(color);
                } else {
                    buttons[row][col].setBackground(Color.BLACK);
                }
            }
        }

        if (isSorted) {
            try {
                Thread.sleep(1000);
                initializeArray(buttons[0].length);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeArray(int length) {
        array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = random.nextInt(length - 2) + 1;
        }
        currentPos = 0;
        comparePos = 1;
        minPos = 0;
        isSorted = false;
    }

    @Override
    public String getName() {
        return "Selection Sort";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.COMPSCI;
    }
}
