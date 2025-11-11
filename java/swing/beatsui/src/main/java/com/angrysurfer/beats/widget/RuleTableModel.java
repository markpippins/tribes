package com.angrysurfer.beats.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Rule;

/**
 * Dedicated table model for handling Rule objects in a JTable
 */
public class RuleTableModel extends AbstractTableModel {
    private static final Logger logger = LoggerFactory.getLogger(RuleTableModel.class.getName());
    
    // Column constants
    public static final int COL_OPERATOR = 0;
    public static final int COL_COMPARISON = 1;
    public static final int COL_VALUE = 2;
    public static final int COL_PART = 3;
    
    private final String[] columnNames = { "Comparison", "Operator", "Value", "Part" };
    private final List<Rule> rules = new ArrayList<>();
    
    @Override
    public int getRowCount() {
        return rules.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rules.size()) {
            return null;
        }
        
        Rule rule = rules.get(rowIndex);
        
        return switch (columnIndex) {
            case COL_OPERATOR -> rule.getOperatorText();     // Property column - "Beat", "Tick", etc.
            case COL_COMPARISON -> rule.getComparisonText(); // Operator column - "==", "<", etc.
            case COL_VALUE -> rule.getValue();               // Value column
            case COL_PART -> rule.getPartText();             // Part column
            default -> null;
        };
    }
    
    /**
     * Clear all data and load a new set of rules
     */
    public void setRules(Set<Rule> newRules) {
        rules.clear();
        
        if (newRules != null && !newRules.isEmpty()) {
            rules.addAll(newRules);
            
            // Sort rules for consistent display order
            rules.sort(Comparator
                .comparingInt(Rule::getOperator)
                .thenComparingDouble(Rule::getValue));
            
            logger.info("Table model loaded with " + rules.size() + " rules");
        } else {
            logger.info("Table model cleared (no rules)");
        }
        
        fireTableDataChanged();
    }
    
    /**
     * Get the rule at the specified row
     */
    public Rule getRuleAt(int row) {
        if (row >= 0 && row < rules.size()) {
            return rules.get(row);
        }
        return null;
    }
    
    /**
     * Find the row index for a rule with the given ID
     */
    public int findRuleRowById(Long ruleId) {
        if (ruleId == null) return -1;
        
        for (int i = 0; i < rules.size(); i++) {
            if (ruleId.equals(rules.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Get rules at the specified rows
     */
    public Rule[] getRulesAt(int[] rows) {
        if (rows == null || rows.length == 0) {
            return new Rule[0];
        }
        
        List<Rule> selectedRules = new ArrayList<>(rows.length);
        for (int row : rows) {
            if (row >= 0 && row < rules.size()) {
                selectedRules.add(rules.get(row));
            }
        }
        
        return selectedRules.toArray(new Rule[0]);
    }
    
    /**
     * Add a single rule to the model
     */
    public void addRule(Rule rule) {
        if (rule != null) {
            rules.add(rule);
            
            // Resort rules
            rules.sort(Comparator
                .comparingInt(Rule::getOperator)
                .thenComparingDouble(Rule::getValue));
            
            fireTableDataChanged();
        }
    }
    
    /**
     * Remove a rule from the model by ID
     */
    public boolean removeRule(Long ruleId) {
        if (ruleId == null) return false;
        
        int index = findRuleRowById(ruleId);
        if (index >= 0) {
            rules.remove(index);
            fireTableRowsDeleted(index, index);
            return true;
        }
        return false;
    }
    
    /**
     * Update a rule in the model
     */
    public boolean updateRule(Rule updatedRule) {
        if (updatedRule == null || updatedRule.getId() == null) return false;
        
        int index = findRuleRowById(updatedRule.getId());
        if (index >= 0) {
            rules.set(index, updatedRule);
            
            // Resort rules as order may have changed
            rules.sort(Comparator
                .comparingInt(Rule::getOperator)
                .thenComparingDouble(Rule::getValue));
            
            fireTableDataChanged();
            return true;
        }
        return false;
    }
}
