package com.angrysurfer.grid.visualization.handler.compsci;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class BubbleSortVisualization implements IVisualizationHandler {
    private int[] array;
    private int currentPos = 0;
    private boolean isSorted = false;
    private final Random random = new Random();
    private static final Color CURRENT_POS_COLOR = Color.RED;
    private static final Color COMPARING_COLOR = Color.YELLOW;
    private static final Color SORTED_COLOR = Color.GREEN;
    private static final Color DEFAULT_COLOR = Color.BLUE;

    @Override
    public void update(JButton[][] buttons) {
        if (array == null || array.length != buttons[0].length) {
            initializeArray(buttons[0].length);
        }

        // Perform one step of bubble sort
        if (!isSorted) {
            boolean swapped = false;
            if (currentPos < array.length - 1) {
                if (array[currentPos] > array[currentPos + 1]) {
                    // Swap elements
                    int temp = array[currentPos];
                    array[currentPos] = array[currentPos + 1];
                    array[currentPos + 1] = temp;
                    swapped = true;
                }
                currentPos++;
            }
            if (currentPos >= array.length - 1) {
                currentPos = 0;
                if (!swapped) {
                    isSorted = true;
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
                    } else if (col == currentPos + 1) {
                        color = COMPARING_COLOR;
                    } else if (isSorted) {
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
        isSorted = false;
    }

    @Override
    public String getName() {
        return "Bubble Sort";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.COMPSCI;
    }
}
