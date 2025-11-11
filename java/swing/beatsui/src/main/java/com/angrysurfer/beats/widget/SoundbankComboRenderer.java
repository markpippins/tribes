package com.angrysurfer.beats.widget;

import com.angrysurfer.core.model.preset.SoundbankItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom renderer for soundbank combo box that formats display names
 */

public class SoundbankComboRenderer extends DefaultListCellRenderer {
    private static final Logger logger = LoggerFactory.getLogger(SoundbankComboRenderer.class);

    // Array of strings to strip from soundbank names - can be edited as needed
    private static final List<String> STRINGS_TO_STRIP = new ArrayList<>(Arrays.asList(
            "General_MIDI",
            "GM_",
            "_Bank",
            "FNF",
            "bs16i",
            "SF2",
            "Soundfont",
            "SoundFont",
            "[2024]"
    ));

    /**
     * Add a string to strip from soundbank names
     *
     * @param str The string to strip
     */
    static void addStringToStrip(String str) {
        if (!STRINGS_TO_STRIP.contains(str)) {
            STRINGS_TO_STRIP.add(str);
        }
    }

    /**
     * Remove a string from the strip list
     *
     * @param str The string to remove
     */
    static void removeStringToStrip(String str) {
        STRINGS_TO_STRIP.remove(str);
    }

    /**
     * Get the current list of strings to strip
     */
    static List<String> getStringsToStrip() {
        return new ArrayList<>(STRINGS_TO_STRIP); // Return copy to prevent direct modification
    }

    /**
     * Format a soundbank name according to the rules
     */
    static String formatSoundbankName(String name) {
        if (name == null) return "";

        // First, strip file extensions
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex > 0) {
            name = name.substring(0, extensionIndex);
        }

        // Replace underscores with spaces
        name = name.replace('_', ' ');
        name = name.replace('-', ' ');

        // Strip configured strings
        for (String strToRemove : STRINGS_TO_STRIP) {
            name = name.replace(strToRemove, "");
        }

        // Clean up double spaces and trim
        name = name.replaceAll("\\s+", " ").trim();

        return name;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {

        // Get the default renderer component
        Component component = super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (value instanceof SoundbankItem item) {
            String originalName = item.getName();
            String formattedName = formatSoundbankName(originalName);

            // Set the formatted text
            setText(formattedName);

            // Set tooltip to show original name
            setToolTipText(originalName);
        }

        return component;
    }
}
