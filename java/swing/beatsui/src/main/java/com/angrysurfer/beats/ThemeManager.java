package com.angrysurfer.beats;

import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

public class ThemeManager {
    private static ThemeManager instance;
    private final JFrame mainFrame;

    private ThemeManager(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public static ThemeManager getInstance(JFrame mainFrame) {
        if (instance == null) {
            instance = new ThemeManager(mainFrame);
        }
        return instance;
    }

    public JMenu createThemeMenu() {
        JMenu themeMenu = new JMenu("Theme");

        // Platform Themes
        JMenu platformThemes = new JMenu("Platform");
        UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
        Arrays.sort(looks, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (UIManager.LookAndFeelInfo look : looks) {
            addThemeItem(platformThemes, look.getName(), look.getClassName());
        }

        // Add basic themes
        JMenu basicThemes = new JMenu("FlatLaf Core");
        addThemeItem(basicThemes, "Darcula", () -> new FlatDarculaLaf());
        addThemeItem(basicThemes, "Dark", () -> new FlatDarkLaf());
        addThemeItem(basicThemes, "IntelliJ", () -> new FlatIntelliJLaf());
        addThemeItem(basicThemes, "Light", () -> new FlatLightLaf());
        addThemeItem(basicThemes, "Mac Dark", () -> new FlatMacDarkLaf());
        addThemeItem(basicThemes, "Mac Light", () -> new FlatMacLightLaf());

        // FlatLaf Themes
        JMenu flatThemes = new JMenu("Intellij");
        JMenu matThemes = new JMenu("Material Lite");

        // Sort and add all IntelliJ themes
        FlatAllIJThemes.FlatIJLookAndFeelInfo[] sortedThemes = FlatAllIJThemes.INFOS.clone();
        Arrays.sort(sortedThemes, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // for themes that do not contain the word "Material" in their name, add to flatThemes  
        for (FlatAllIJThemes.FlatIJLookAndFeelInfo info : sortedThemes) {
            if (!info.getName().toLowerCase().contains("material")) {
                addThemeItem(flatThemes, info.getName(), info.getClassName());
            }
        }

        // for themes that contain the word "Material" in their name, add to matThemes  
        for (FlatAllIJThemes.FlatIJLookAndFeelInfo info : sortedThemes) {
            if (info.getName().toLowerCase().contains("material")) {
                addThemeItem(matThemes, info.getName(), info.getClassName());
            }
        }

        themeMenu.add(basicThemes);
        themeMenu.add(flatThemes);
        themeMenu.add(matThemes);
        themeMenu.add(platformThemes);

        return themeMenu;
    }

    private void addThemeItem(JMenu menu, String name, String className) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> setTheme(className));
        menu.add(item);
    }

    private void addThemeItem(JMenu menu, String name, ThemeSupplier supplier) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> setTheme(supplier));
        menu.add(item);
    }

    private void setTheme(String className) {
        try {
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(mainFrame);
            notifyThemeChange(className);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ThemeManager.class).error("Failed to set theme {}", className, ex);
        }
    }

    private void setTheme(ThemeSupplier supplier) {
        try {
            LookAndFeel laf = supplier.get();
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(mainFrame);
            notifyThemeChange(laf.getClass().getName());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ThemeManager.class).error("Failed to set theme via supplier", ex);
        }
    }

    private void notifyThemeChange(String themeName) {
        CommandBus.getInstance().publish(Commands.CHANGE_THEME, this, themeName);
    }

    @FunctionalInterface
    private interface ThemeSupplier {
        LookAndFeel get();
    }
}
