package com.angrysurfer.beats.panel.sample;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File browser panel using JTree with icons and navigation features
 */
public class FileBrowserPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FileBrowserPanel.class);
    
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private JTextField pathField;
    private JButton upButton;
    private JButton homeButton;
    private JButton refreshButton;
    private Consumer<File> onFileSelectedCallback;
    
    private File currentDirectory;
    
    public FileBrowserPanel(Consumer<File> onFileSelectedCallback) {
        this.onFileSelectedCallback = onFileSelectedCallback;
        
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create and configure navigation panel (top)
        JPanel navigationPanel = createNavigationPanel();
        add(navigationPanel, BorderLayout.NORTH);
        
        // Create file tree
        fileTree = createFileTree();
        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        add(treeScrollPane, BorderLayout.CENTER);
        
        // Set minimum size for the panel
        setMinimumSize(new java.awt.Dimension(200, 400));
    }
    
    /**
     * Create the navigation panel with path field and buttons
     */
    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        // Create path field
        pathField = new JTextField();
        pathField.setEditable(false);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        // Up button
        upButton = new JButton("\u2191");
        upButton.setToolTipText("Go up one level");
        upButton.addActionListener(e -> navigateUp());
        
        // Home button
        homeButton = new JButton("\u2302");
        homeButton.setToolTipText("Go to home directory");
        homeButton.addActionListener(e -> navigateToDirectory(
                Path.of(System.getProperty("user.home"))));
        
        // Refresh button
        refreshButton = new JButton("\u21BB");
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(e -> refreshCurrentDirectory());
        
        // Add buttons to panel
        buttonPanel.add(upButton);
        buttonPanel.add(homeButton);
        buttonPanel.add(refreshButton);
        
        // Add components to navigation panel
        panel.add(pathField, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Create the file tree with custom renderer
     */
    private JTree createFileTree() {
        // Create root node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(rootNode);
        
        // Create tree with model
        JTree tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        
        // Set custom cell renderer with icons
        tree.setCellRenderer(new FileTreeCellRenderer());
        
        // Add selection listener
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        tree.getLastSelectedPathComponent();
                
                if (node == null) return;
                
                Object userObject = node.getUserObject();
                if (userObject instanceof FileNode fileNode) {
                    File file = fileNode.getFile();
                    
                    if (file.isDirectory()) {
                        navigateToDirectory(file.toPath());
                    } else {
                        // Notify callback about file selection
                        if (onFileSelectedCallback != null) {
                            onFileSelectedCallback.accept(file);
                        }
                    }
                }
            }
        });
        
        // Add double-click listener for directories
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                                path.getLastPathComponent();
                        
                        if (node == null) return;
                        
                        Object userObject = node.getUserObject();
                        if (userObject instanceof FileNode fileNode) {
                            File file = fileNode.getFile();
                            
                            if (file.isDirectory()) {
                                navigateToDirectory(file.toPath());
                            }
                        }
                    }
                }
            }
        });
        
        // Add expansion listener to load directory contents when expanded
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                
                if (node != null && node.getUserObject() instanceof FileNode) {
                    // Load directory contents
                    loadDirectory((FileNode) node.getUserObject(), node);
                }
            }
            
            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                // No action needed
            }
        });
        
        return tree;
    }
    
    /**
     * Navigate to specified directory and populate the tree
     */
    public void navigateToDirectory(Path path) {
        File directory = path.toFile();
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Invalid directory: {}", path);
            return;
        }
        
        // Update current directory
        currentDirectory = directory;
        
        // Update path field
        pathField.setText(directory.getAbsolutePath());
        
        // Clear root node
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        rootNode.removeAllChildren();
        
        // Add file nodes
        File[] files = directory.listFiles(file -> {
            return file.isDirectory() || file.getName().toLowerCase().endsWith(".wav");
        });
        
        if (files != null) {
            // Sort files (directories first, then files)
            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            
            // Add nodes to tree
            for (File file : files) {
                FileNode fileNode = new FileNode(file);
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(fileNode);
                
                // If directory, add dummy node to enable expansion
                if (file.isDirectory()) {
                    treeNode.add(new DefaultMutableTreeNode("Loading..."));
                }
                
                rootNode.add(treeNode);
            }
        }
        
        // Notify model and expand root
        treeModel.reload(rootNode);
        fileTree.expandPath(new TreePath(rootNode.getPath()));
    }
    
    /**
     * Load directory contents for the specified node
     */
    private void loadDirectory(FileNode fileNode, DefaultMutableTreeNode parentNode) {
        File directory = fileNode.getFile();
        
        // Check if already loaded (has real children, not dummy)
        if (parentNode.getChildCount() == 1) {
            Object userObject = ((DefaultMutableTreeNode) parentNode.getChildAt(0)).getUserObject();
            if (!(userObject instanceof FileNode)) {
                // Remove dummy node
                parentNode.removeAllChildren();
                
                // Load actual content
                File[] files = directory.listFiles(file -> {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith(".wav");
                });
                
                if (files != null) {
                    // Sort files (directories first, then files)
                    java.util.Arrays.sort(files, (a, b) -> {
                        if (a.isDirectory() && !b.isDirectory()) return -1;
                        if (!a.isDirectory() && b.isDirectory()) return 1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    });
                    
                    // Add nodes
                    for (File file : files) {
                        FileNode childFileNode = new FileNode(file);
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childFileNode);
                        
                        // If directory, add dummy node to enable expansion
                        if (file.isDirectory()) {
                            childNode.add(new DefaultMutableTreeNode("Loading..."));
                        }
                        
                        parentNode.add(childNode);
                    }
                }
                
                // Notify model
                treeModel.nodeStructureChanged(parentNode);
            }
        }
    }
    
    /**
     * Navigate up one directory level
     */
    private void navigateUp() {
        if (currentDirectory != null) {
            File parent = currentDirectory.getParentFile();
            if (parent != null) {
                navigateToDirectory(parent.toPath());
            }
        }
    }
    
    /**
     * Refresh the current directory view
     */
    private void refreshCurrentDirectory() {
        if (currentDirectory != null) {
            navigateToDirectory(currentDirectory.toPath());
        }
    }
    
    /**
     * Refresh UI after theme change
     */
    public void refreshUI() {
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }
    
    /**
     * Custom file node class to hold file reference
     */
    private static class FileNode {
        private final File file;
        
        public FileNode(File file) {
            this.file = file;
        }
        
        public File getFile() {
            return file;
        }
        
        @Override
        public String toString() {
            return file.getName();
        }
    }
    
    /**
     * Custom renderer for file tree with icons
     */
    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;
        
        private static final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
        private static final Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
        private static final Icon audioIcon; // Custom icon for audio files
        
        static {
            // Try to load custom audio icon
            Icon tempIcon = null;
            try {
                // Load from resources if available
                tempIcon = new ImageIcon(FileTreeCellRenderer.class.getResource("/icons/audio-file.png"));
            } catch (Exception e) {
                // Fall back to default file icon
                tempIcon = fileIcon;
            }
            audioIcon = tempIcon;
        }
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, selected,
                    expanded, leaf, row, hasFocus);
            
            // Set icon based on file type
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof FileNode fileNode) {
                File file = fileNode.getFile();
                
                if (file.isDirectory()) {
                    setIcon(folderIcon);
                } else if (file.getName().toLowerCase().endsWith(".wav")) {
                    setIcon(audioIcon);
                } else {
                    setIcon(fileIcon);
                }
            } else if (userObject instanceof String && "Loading...".equals(userObject)) {
                setText("Loading...");
                setIcon(null);
            }
            
            return this;
        }
    }
}