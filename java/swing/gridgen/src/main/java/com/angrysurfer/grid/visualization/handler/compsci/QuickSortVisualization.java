package com.angrysurfer.grid.visualization.handler.compsci;

import java.awt.Color;
import java.util.Random;
import java.util.Stack;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class QuickSortVisualization implements IVisualizationHandler {
    private int[] array;
    private final Random random = new Random();
    private final Stack<int[]> ranges = new Stack<>();
    private int pivotIndex = -1;
    private int currentIndex = -1;
    private int leftBound = -1;
    private int rightBound = -1;
    private boolean isSorted = false;
    private int[] currentRange;  // Add this field
    
    private static final Color PIVOT_COLOR = Color.RED;
    private static final Color CURRENT_COLOR = Color.YELLOW;
    private static final Color RANGE_COLOR = Color.MAGENTA;
    private static final Color SORTED_COLOR = Color.GREEN;
    private static final Color DEFAULT_COLOR = Color.BLUE;

    @Override
    public void update(JButton[][] buttons) {
        if (array == null || array.length != buttons[0].length) {
            initializeArray(buttons[0].length);
        }

        if (!isSorted) {
            if (ranges.isEmpty() && leftBound == -1) {
                ranges.push(new int[]{0, array.length - 1});
            }

            if (leftBound == -1 && !ranges.isEmpty()) {
                currentRange = ranges.pop();  // Store the current range
                leftBound = currentRange[0];
                rightBound = currentRange[1];
                pivotIndex = rightBound;
                currentIndex = leftBound;
            }

            if (leftBound < rightBound) {
                // Partitioning in progress
                if (currentIndex < rightBound) {
                    if (array[currentIndex] <= array[pivotIndex]) {
                        int temp = array[leftBound];
                        array[leftBound] = array[currentIndex];
                        array[currentIndex] = temp;
                        leftBound++;
                    }
                    currentIndex++;
                } else {
                    // Partition complete, swap pivot
                    int temp = array[leftBound];
                    array[leftBound] = array[pivotIndex];
                    array[pivotIndex] = temp;

                    // Queue up sub-partitions
                    if (leftBound - 1 > currentRange[0]) {  // Use currentRange here
                        ranges.push(new int[]{currentRange[0], leftBound - 1});
                    }
                    if (leftBound + 1 < currentRange[1]) {  // And here
                        ranges.push(new int[]{leftBound + 1, currentRange[1]});
                    }

                    // Reset for next partition
                    leftBound = -1;
                    rightBound = -1;
                    pivotIndex = -1;
                    currentIndex = -1;
                    currentRange = null;  // Clear current range
                }
            } else {
                if (ranges.isEmpty()) {
                    isSorted = true;
                }
            }
        }

        // Visualize current state
        for (int col = 0; col < buttons[0].length; col++) {
            int height = array[col];
            for (int row = 0; row < buttons.length; row++) {
                Color color = DEFAULT_COLOR;
                if (row >= buttons.length - height) {
                    if (col == pivotIndex) {
                        color = PIVOT_COLOR;
                    } else if (col == currentIndex) {
                        color = CURRENT_COLOR;
                    } else if (col >= leftBound && col <= rightBound) {
                        color = RANGE_COLOR;
                    } else if (isSorted || !ranges.isEmpty() && col < ranges.firstElement()[0]) {
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
        ranges.clear();
        pivotIndex = -1;
        currentIndex = -1;
        leftBound = -1;
        rightBound = -1;
        isSorted = false;
        currentRange = null;  // Add this line
    }

    @Override
    public String getName() {
        return "Quick Sort";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.COMPSCI;
    }
}
