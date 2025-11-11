package com.angrysurfer.beats.widget;

import com.angrysurfer.core.model.Rule;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom renderer for rule table rows that formats values appropriately
 * and handles highlighting for selected or flashing rows.
 */
@Getter
@Setter
public class RulesRowRenderer extends DefaultTableCellRenderer {

    private final RulesTable table;

    public RulesRowRenderer(RulesTable table) {
        this.table = table;
    }

    @Override
    public Component getTableCellRendererComponent(JTable ignored, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        // Use our table reference instead of the provided one
        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (c instanceof JLabel label) {
            Rule rule = table.getTableModel().getRuleAt(row);

            if (rule == null) {
                return c;
            }

            // Set alignment based on column
            if (column == 0) {
                // Operator column - left align
                label.setHorizontalAlignment(JLabel.LEFT);
            } else {
                // Other columns - center align
                label.setHorizontalAlignment(JLabel.CENTER);
            }

            // Format value for Part column
            if (column == 3 && "0".equals(value.toString())) {
                label.setText("All");
            }

            // Handle background color for flashing or selected
            if (table.isRuleFlashing(rule)) {
                // Use flash color for background
                label.setBackground(isSelected ?
                        table.getFlashColor().darker() :
                        table.getFlashColor());
                label.setForeground(Color.BLACK);
            } else {
                // Use normal selection colors
                label.setBackground(isSelected ?
                        table.getSelectionBackground() :
                        table.getBackground());
                label.setForeground(isSelected ?
                        table.getSelectionForeground() :
                        table.getForeground());
            }
        }

        return c;
    }
}
