package com.angrysurfer.beats.panel.sequencer;
// package com.angrysurfer.beats.panel;

// import java.awt.Color;
// import java.awt.Dimension;
// import java.awt.Font;
// import java.awt.Graphics;
// import java.awt.Graphics2D;
// import java.awt.Point;
// import java.awt.RenderingHints;
// import java.awt.event.MouseAdapter;
// import java.awt.event.MouseEvent;
// import java.awt.event.MouseMotionAdapter;
// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.List;

// import javax.swing.JPanel;

// import com.angrysurfer.beats.widget.UIHelper;
// import com.angrysurfer.core.sequencer.MelodicSequencer;
// import com.angrysurfer.core.sequencer.PatternSlot;
// import com.angrysurfer.core.service.SessionManager;

// /**
//  * Internal panel for custom timeline drawing
//  */
// class SongTimelinePanel extends JPanel {
    
//     /**
//      *
//      */
//     private final SongPanel songPanel;

//     public SongTimelinePanel(SongPanel songPanel) {
//         this.songPanel = songPanel;
//         setLayout(null); // Absolute positioning for pattern blocks
//         setBackground(UIHelper.getBackgroundColor());
        
//         // Track total components (1 drum track + melodic sequencers)
//         int trackCount = 1 + patternSequencer.getMelodicSequencers().size();
        
//         // Set preferred size based on song length and track count
//         int width = SongPanel.TRACK_HEADER_WIDTH + (this.songPanel.songLength * SongPanel.BAR_WIDTH);
//         int height = SongPanel.HEADER_HEIGHT + (trackCount * SongPanel.TRACK_HEIGHT);
//         setPreferredSize(new Dimension(width, height));
        
//         // Mouse listeners for drag & drop
//         addMouseListener(new MouseAdapter() {
//             @Override
//             public void mousePressed(MouseEvent e) {
//                 // Check if clicking on a pattern slot
//                 PatternSlot slot = findPatternSlotAt(e.getPoint());
//                 if (slot != null) {
//                     SongTimelinePanel.this.songPanel.selectedSlot = slot;
//                     SongTimelinePanel.this.songPanel.draggingSlot = slot;
//                     SongTimelinePanel.this.songPanel.isDragging = true;
//                     SongTimelinePanel.this.songPanel.dragOffset.x = e.getX() - (SongPanel.TRACK_HEADER_WIDTH + (slot.getPosition() * SongPanel.BAR_WIDTH));
//                     SongTimelinePanel.this.songPanel.deletePatternButton.setEnabled(true);
//                     repaint();
//                 } else {
//                     // Clicked on empty space
//                     SongTimelinePanel.this.songPanel.selectedSlot = null;
//                     SongTimelinePanel.this.songPanel.deletePatternButton.setEnabled(false);
//                     repaint();
//                 }
//             }
            
//             @Override
//             public void mouseReleased(MouseEvent e) {
//                 if (SongTimelinePanel.this.songPanel.isDragging && SongTimelinePanel.this.songPanel.draggingSlot != null) {
//                     // Calculate new position based on BAR_WIDTH
//                     int newX = e.getX() - SongTimelinePanel.this.songPanel.dragOffset.x - SongPanel.TRACK_HEADER_WIDTH;
//                     int newPosition = Math.max(0, newX / SongPanel.BAR_WIDTH);
//                     SongTimelinePanel.this.songPanel.draggingSlot.setPosition(newPosition);
                    
//                     // Ensure patterns don't overlap
//                     if ("DRUM".equals(SongTimelinePanel.this.songPanel.draggingSlot.getType())) {
//                         resolveOverlaps(SongTimelinePanel.this.songPanel.drumPatternSlots, SongTimelinePanel.this.songPanel.draggingSlot);
//                     } else if ("MELODIC".equals(SongTimelinePanel.this.songPanel.draggingSlot.getType()) && SongTimelinePanel.this.songPanel.draggingSlot.getSequencerId() != null) {
//                         List<PatternSlot> slots = SongTimelinePanel.this.songPanel.melodicPatternSlots.get(SongTimelinePanel.this.songPanel.draggingSlot.getSequencerId());
//                         if (slots != null) {
//                             resolveOverlaps(slots, SongTimelinePanel.this.songPanel.draggingSlot);
//                         }
//                     }
//                 }
                
//                 SongTimelinePanel.this.songPanel.isDragging = false;
//                 SongTimelinePanel.this.songPanel.draggingSlot = null;
//                 repaint();
//             }
//         });
        
//         addMouseMotionListener(new MouseMotionAdapter() {
//             @Override
//             public void mouseDragged(MouseEvent e) {
//                 if (SongTimelinePanel.this.songPanel.isDragging && SongTimelinePanel.this.songPanel.draggingSlot != null) {
//                     // Update selection rectangle during dragging
//                     repaint();
//                 }
//             }
//         });
//     }
    
//     @Override
//     protected void paintComponent(Graphics g) {
//         super.paintComponent(g);
//         Graphics2D g2d = (Graphics2D) g;
//         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
//         // Draw track headers background
//         g2d.setColor(UIHelper.getAccentColor());
//         g2d.fillRect(0, SongPanel.HEADER_HEIGHT, SongPanel.TRACK_HEADER_WIDTH, getHeight());
        
//         // Draw timeline header background
//         g2d.setColor(UIHelper.getAccentColor().darker());
//         g2d.fillRect(0, 0, getWidth(), SongPanel.HEADER_HEIGHT);
        
//         // Draw bar numbers
//         g2d.setColor(UIHelper.getTextColor());
//         g2d.setFont(new Font("Dialog", Font.PLAIN, 12));
//         for (int i = 0; i < this.songPanel.songLength; i++) {
//             int x = SongPanel.TRACK_HEADER_WIDTH + (i * SongPanel.BAR_WIDTH) + (SongPanel.BAR_WIDTH / 2) - 10;
//             g2d.drawString(Integer.toString(i + 1), x, 20);
//         }
        
//         // Draw bar dividers
//         g2d.setColor(UIHelper.getTextColor().darker());
//         for (int i = 0; i <= this.songPanel.songLength; i++) {
//             int x = SongPanel.TRACK_HEADER_WIDTH + (i * SongPanel.BAR_WIDTH);
//             g2d.drawLine(x, 0, x, getHeight());
//         }
        
//         // Draw track names
//         int trackY = SongPanel.HEADER_HEIGHT;
//         g2d.setColor(UIHelper.getTextColor());
        
//         // Drum track
//         g2d.drawString("Drums", 10, trackY + (SongPanel.TRACK_HEIGHT / 2));
//         trackY += SongPanel.TRACK_HEIGHT;
        
//         // Melodic tracks
//         for (int i = 0; i < patternSequencer.getMelodicSequencers().size(); i++) {
//             g2d.drawString("Mono " + (i + 1), 10, trackY + (SongPanel.TRACK_HEIGHT / 2));
//             trackY += SongPanel.TRACK_HEIGHT;
//         }
        
//         // Draw track dividers
//         g2d.setColor(UIHelper.getTextColor().darker());
//         trackY = SongPanel.HEADER_HEIGHT;
//         for (int i = 0; i <= 1 + patternSequencer.getMelodicSequencers().size(); i++) {
//             g2d.drawLine(0, trackY, getWidth(), trackY);
//             trackY += SongPanel.TRACK_HEIGHT;
//         }
        
//         // Highlight current bar (adjust for 0-based display)
//         if (SessionManager.getInstance().getActiveSession().isRunning()) {
//             g2d.setColor(new Color(255, 255, 255, 50)); // Semi-transparent white
//             int zeroBasedCurrentBar = this.songPanel.currentBar - 1;
//             int barX = SongPanel.TRACK_HEADER_WIDTH + (zeroBasedCurrentBar * SongPanel.BAR_WIDTH);
//             g2d.fillRect(barX, SongPanel.HEADER_HEIGHT, SongPanel.BAR_WIDTH, getHeight() - SongPanel.HEADER_HEIGHT);
//         }
        
//         // Draw drum pattern slots
//         trackY = SongPanel.HEADER_HEIGHT;
//         drawPatternSlots(g2d, this.songPanel.drumPatternSlots, trackY, UIHelper.warmMustard);
        
//         // Draw melodic pattern slots
//         trackY += SongPanel.TRACK_HEIGHT;
//         for (MelodicSequencer sequencer : patternSequencer.getMelodicSequencers()) {
//             List<PatternSlot> slots = this.songPanel.melodicPatternSlots.get(sequencer.getId());
//             if (slots != null) {
//                 drawPatternSlots(g2d, slots, trackY, UIHelper.coolBlue);
//             }
//             trackY += SongPanel.TRACK_HEIGHT;
//         }
        
//         // Draw dragging slot if active
//         if (this.songPanel.isDragging && this.songPanel.draggingSlot != null) {
//             // Get track Y based on type and sequencer ID
//             int dragTrackY = getDragTrackY(this.songPanel.draggingSlot);
            
//             // Calculate position based on mouse position
//             Point mousePos = getMousePosition();
//             if (mousePos != null) {
//                 int dragX = mousePos.x - this.songPanel.dragOffset.x;
                
//                 // Draw semi-transparent dragging slot
//                 Color slotColor = "DRUM".equals(this.songPanel.draggingSlot.getType()) ? 
//                                    UIHelper.warmMustard : UIHelper.coolBlue;
                
//                 g2d.setColor(new Color(slotColor.getRed(), slotColor.getGreen(), 
//                                      slotColor.getBlue(), 128));
                
//                 int slotWidth = this.songPanel.draggingSlot.getLength() * SongPanel.BAR_WIDTH;
//                 g2d.fillRect(dragX, dragTrackY + 5, slotWidth, SongPanel.TRACK_HEIGHT - 10);
                
//                 // Draw border and pattern ID
//                 g2d.setColor(Color.WHITE);
//                 g2d.drawRect(dragX, dragTrackY + 5, slotWidth, SongPanel.TRACK_HEIGHT - 10);
                
//                 String patternText = this.songPanel.draggingSlot.getPatternId().toString();
//                 g2d.drawString(patternText, dragX + 10, dragTrackY + (SongPanel.TRACK_HEIGHT / 2));
//             }
//         }
//     }
    
//     private void drawPatternSlots(Graphics2D g2d, List<PatternSlot> slots, int trackY, Color baseColor) {
//         for (PatternSlot slot : slots) {
//             int x = SongPanel.TRACK_HEADER_WIDTH + (slot.getPosition() * SongPanel.BAR_WIDTH);
//             int width = slot.getLength() * SongPanel.BAR_WIDTH;
            
//             // Draw pattern block background
//             Color fillColor = slot == this.songPanel.selectedSlot ? baseColor.brighter() : baseColor;
//             g2d.setColor(fillColor);
//             g2d.fillRect(x, trackY + 5, width, SongPanel.TRACK_HEIGHT - 10);
            
//             // Draw border
//             g2d.setColor(Color.WHITE);
//             g2d.drawRect(x, trackY + 5, width, SongPanel.TRACK_HEIGHT - 10);
            
//             // Draw pattern info
//             g2d.setFont(new Font("Dialog", Font.BOLD, 12));
//             g2d.drawString("Pattern " + slot.getPatternId(), x + 10, trackY + (SongPanel.TRACK_HEIGHT / 2));
//         }
//     }
    
//     private PatternSlot findPatternSlotAt(Point point) {
//         // Calculate which track the point is in
//         int trackIndex = (point.y - SongPanel.HEADER_HEIGHT) / SongPanel.TRACK_HEIGHT;
        
//         // Calculate bar position
//         int barPos = (point.x - SongPanel.TRACK_HEADER_WIDTH) / SongPanel.BAR_WIDTH;
        
//         // Check if point is within pattern slots
//         if (trackIndex == 0) {
//             // Drum track
//             return findSlotContaining(this.songPanel.drumPatternSlots, barPos);
//         } else if (trackIndex > 0 && trackIndex <= patternSequencer.getMelodicSequencers().size()) {
//             // Melodic track
//             MelodicSequencer sequencer = patternSequencer.getMelodicSequencers().get(trackIndex - 1);
//             List<PatternSlot> slots = this.songPanel.melodicPatternSlots.get(sequencer.getId());
//             return findSlotContaining(slots, barPos);
//         }
        
//         return null;
//     }
    
//     private PatternSlot findSlotContaining(List<PatternSlot> slots, int position) {
//         if (slots == null) return null;
        
//         for (PatternSlot slot : slots) {
//             if (position >= slot.getPosition() && position < slot.getPosition() + slot.getLength()) {
//                 return slot;
//             }
//         }
//         return null;
//     }
    
//     private int getDragTrackY(PatternSlot slot) {
//         if ("DRUM".equals(slot.getType())) {
//             return SongPanel.HEADER_HEIGHT;
//         } else if ("MELODIC".equals(slot.getType()) && slot.getSequencerId() != null) {
//             // Find the index of the sequencer
//             for (int i = 0; i < patternSequencer.getMelodicSequencers().size(); i++) {
//                 if (patternSequencer.getMelodicSequencers().get(i).getId().equals(slot.getSequencerId())) {
//                     return SongPanel.HEADER_HEIGHT + ((i + 1) * SongPanel.TRACK_HEIGHT);
//                 }
//             }
//         }
//         return SongPanel.HEADER_HEIGHT;
//     }
    
//     private void resolveOverlaps(List<PatternSlot> slots, PatternSlot movedSlot) {
//         // Sort by position
//         List<PatternSlot> sortedSlots = new ArrayList<>(slots);
//         sortedSlots.sort(Comparator.comparing(PatternSlot::getPosition));
        
//         // Check for and resolve overlaps
//         for (int i = 0; i < sortedSlots.size() - 1; i++) {
//             PatternSlot current = sortedSlots.get(i);
//             PatternSlot next = sortedSlots.get(i + 1);
            
//             // Skip if comparing the same slot
//             if (current == next) continue;
            
//             // If current pattern overlaps with next pattern
//             int currentEnd = current.getPosition() + current.getLength();
//             if (currentEnd > next.getPosition()) {
//                 // If moved slot is the second one, push it forward
//                 if (movedSlot == next) {
//                     next.setPosition(currentEnd);
//                 } 
//                 // If moved slot is the first one, truncate it
//                 else if (movedSlot == current) {
//                     current.setLength(next.getPosition() - current.getPosition());
//                 }
//             }
//         }
//     }
    
//     @Override
//     public void setBounds(int x, int y, int width, int height) {
//         super.setBounds(x, y, width, height);
        
//         // Update preferred size when resized
//         int requiredWidth = SongPanel.TRACK_HEADER_WIDTH + (this.songPanel.songLength * SongPanel.BAR_WIDTH);
//         int requiredHeight = SongPanel.HEADER_HEIGHT + ((1 + patternSequencer.getMelodicSequencers().size()) * SongPanel.TRACK_HEIGHT);
//         setPreferredSize(new Dimension(Math.max(width, requiredWidth), Math.max(height, requiredHeight)));
//     }
// }
