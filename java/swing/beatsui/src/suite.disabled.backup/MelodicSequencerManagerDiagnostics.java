package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.SequencerService;
import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.redis.RedisService;

import java.util.List;

/**
 * Helper class for MelodicSequencerManager diagnostics
 */
public class MelodicSequencerManagerDiagnostics {
    
    /**
     * Run diagnostics on the MelodicSequencerManager
     */
    public DiagnosticLogBuilder testMelodicSequencerManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MelodicSequencerManager Diagnostics");
        
        try {
            // Get the manager instance
            MelodicSequencerManager manager = SequencerService.getInstance();
            
            if (manager == null) {
                log.addError("Failed to get MelodicSequencerManager instance");
                return log;
            }
            
            // 1. Check sequencer instances
            log.addSection("1. Sequencer Instances");
            
            List<MelodicSequencer> sequencers = manager.getAllSequencers();
            if (sequencers == null) {
                log.addError("Sequencer list is null");
            } else {
                log.addIndentedLine("Found " + sequencers.size() + " melodic sequencer(s)", 1);
                
                for (int i = 0; i < sequencers.size(); i++) {
                    MelodicSequencer sequencer = sequencers.get(i);
                    log.addIndentedLine("Sequencer " + i + ": ID=" + sequencer.getId(), 1);
                    
                    // Check if player and instrument are set
                    if (sequencer.getPlayer() == null) {
                        log.addWarning("Sequencer " + i + " has no player assigned");
                    } else if (sequencer.getPlayer().getInstrument() == null) {
                        log.addWarning("Sequencer " + i + " has player but no instrument");
                    }
                    
                    // Check sequence data
                    MelodicSequenceData data = sequencer.getSequenceData();
                    if (data == null) {
                        log.addError("Sequencer " + i + " has null sequence data");
                    } else {
                        log.addIndentedLine("Sequence ID: " + data.getId(), 2);
                        
                        // Check for active steps
                        int activeSteps = 0;
                        for (int step = 0; step < data.getPatternLength(); step++) {
                            if (data.isStepActive(step)) {
                                activeSteps++;
                            }
                        }
                        log.addIndentedLine("Active steps: " + activeSteps + " of " + data.getPatternLength(), 2);
                    }
                }
            }
            
            // 2. Check saved sequences
            log.addSection("2. Saved Sequences");
            
            // Use first sequencer's ID to test manager functions
            Integer testSequencerId = null;
            if (sequencers != null && !sequencers.isEmpty()) {
                testSequencerId = sequencers.get(0).getId();
            }
            
            if (testSequencerId != null) {
                log.addIndentedLine("Testing with sequencer ID: " + testSequencerId, 1);
                
                // Test sequence listing
                List<Long> sequenceIds = manager.getAllMelodicSequenceIds(testSequencerId);
                if (sequenceIds == null) {
                    log.addError("Failed to get sequence IDs");
                } else {
                    log.addIndentedLine("Found " + sequenceIds.size() + " saved sequence(s)", 1);
                    
                    // Display first few sequence IDs
                    int displayCount = Math.min(sequenceIds.size(), 5);
                    for (int i = 0; i < displayCount; i++) {
                        log.addIndentedLine("Sequence " + i + ": ID=" + sequenceIds.get(i), 2);
                    }
                    
                    if (sequenceIds.size() > displayCount) {
                        log.addIndentedLine("... and " + (sequenceIds.size() - displayCount) + " more", 2);
                    }
                    
                    // Test sequence retrieval
                    if (!sequenceIds.isEmpty()) {
                        log.addIndentedLine("Testing sequence retrieval", 1);
                        Long testSequenceId = sequenceIds.get(0);
                        
                        MelodicSequenceData sequence = RedisService.getInstance().findMelodicSequenceById(
                                testSequenceId, testSequencerId);
                        
                        if (sequence == null) {
                            log.addError("Failed to retrieve sequence with ID " + testSequenceId);
                        } else {
                            log.addIndentedLine("Successfully retrieved sequence: " + sequence.getName(), 2);
                            log.addIndentedLine("Created: " + new java.util.Date(sequence.getCreatedAt()), 2);
                            log.addIndentedLine("Updated: " + new java.util.Date(sequence.getUpdatedAt()), 2);
                            log.addIndentedLine("Pattern Length: " + sequence.getPatternLength(), 2);
                        }
                    }
                }
                
                // Test sequence saving (using current data from sequencer)
                log.addIndentedLine("Testing sequence saving", 1);
                
                MelodicSequencer testSequencer = manager.getSequencer(testSequencerId);
                if (testSequencer != null) {
                    try {
                        // Mark the current time to verify update
                        testSequencer.getSequenceData().setUpdatedAt(System.currentTimeMillis());
                        
                        boolean saved = manager.saveSequence(testSequencer) != null;
                        if (saved) {
                            log.addIndentedLine("Successfully saved sequence", 2);
                        } else {
                            log.addError("Failed to save sequence");
                        }
                    } catch (Exception e) {
                        log.addError("Error saving sequence: " + e.getMessage());
                    }
                } else {
                    log.addError("Failed to get test sequencer with ID " + testSequencerId);
                }
            } else {
                log.addWarning("No sequencers available to test sequence operations");
            }
            
            // 3. Test sequence creation
            log.addSection("3. Sequence Creation");
            
            if (testSequencerId != null) {
                try {
                    MelodicSequenceData newSequence = RedisService.getInstance().newMelodicSequence(testSequencerId);
                    
                    if (newSequence != null) {
                        log.addIndentedLine("Successfully created new sequence with ID: " + newSequence.getId(), 1);
                        
                        // Test sequence application to sequencer
                        MelodicSequencer testSequencer = manager.getSequencer(testSequencerId);
                        if (testSequencer != null) {
                            log.addIndentedLine("Testing sequence application to sequencer", 1);
                            
                            try {
                                boolean wasPlaying = testSequencer.isPlaying();
                                if (wasPlaying) {
                                    testSequencer.stop();
                                }
                                
                                RedisService.getInstance().applyMelodicSequenceToSequencer(newSequence, testSequencer);
                                log.addIndentedLine("Successfully applied sequence to sequencer", 2);
                                
                                // Verify application
                                if (testSequencer.getSequenceData() != null && 
                                    testSequencer.getSequenceData().getId().equals(newSequence.getId())) {
                                    log.addIndentedLine("Sequence ID verification successful", 2);
                                } else {
                                    log.addError("Sequence ID verification failed");
                                }
                                
                                // Restore playing state
                                if (wasPlaying) {
                                    testSequencer.start();
                                }
                            } catch (Exception e) {
                                log.addError("Error applying sequence: " + e.getMessage());
                            }
                        }
                        
                        // Clean up by removing the test sequence
                        try {
                            RedisService.getInstance().deleteMelodicSequence(testSequencerId, newSequence.getId());
                            log.addIndentedLine("Cleaned up test sequence", 1);
                        } catch (Exception e) {
                            log.addWarning("Error cleaning up test sequence: " + e.getMessage());
                        }
                    } else {
                        log.addError("Failed to create new sequence");
                    }
                } catch (Exception e) {
                    log.addError("Error in sequence creation test: " + e.getMessage());
                }
            }
            
            log.addLine("\nMelodicSequencerManager diagnostics completed.");
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test sequence persistence
     */
    public DiagnosticLogBuilder testSequencePersistence() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Melodic Sequence Persistence Test");
        
        try {
            // Get the manager instance
            MelodicSequencerManager manager = SequencerService.getInstance();
            
            if (manager == null) {
                log.addError("Failed to get MelodicSequencerManager instance");
                return log;
            }
            
            // Find a sequencer to test with
            List<MelodicSequencer> sequencers = manager.getAllSequencers();
            if (sequencers == null || sequencers.isEmpty()) {
                log.addError("No sequencers available for testing");
                return log;
            }
            
            MelodicSequencer testSequencer = sequencers.get(0);
            Integer testSequencerId = testSequencer.getId();
            
            log.addLine("Testing with sequencer ID: " + testSequencerId);
            
            // 1. Create a test sequence
            log.addSection("1. Creating Test Sequence");
            
            MelodicSequenceData testSequence = null;
            try {
                testSequence = RedisService.getInstance().newMelodicSequence(testSequencerId);
                
                if (testSequence != null) {
                    log.addIndentedLine("Created test sequence with ID: " + testSequence.getId(), 1);
                    
                    // Modify the sequence with specific test data
                    testSequence.setName("Diagnostic Test Sequence");
                    
                    // Create a simple pattern
                    for (int i = 0; i < 16; i++) {
                        testSequence.setStepActive(i, i % 4 == 0); // Quarter notes
                        testSequence.setNoteValue(i, 60 + (i % 12)); // Note scale
                        testSequence.setVelocityValue(i, 100); // Full velocity
                        testSequence.setGateValue(i, 80); // Long gates
                    }
                    
                    log.addIndentedLine("Configured test sequence with quarter note pattern", 1);
                } else {
                    log.addError("Failed to create test sequence");
                    return log;
                }
            } catch (Exception e) {
                log.addError("Error creating test sequence: " + e.getMessage());
                return log;
            }
            
            // 2. Save the sequence
            log.addSection("2. Saving Sequence");
            
            try {
                // CRITICAL FIX: Apply the test sequence to the sequencer before saving
                testSequencer.setSequenceData(testSequence);
                
                // Now save (the sequencer contains the test sequence)
                RedisService.getInstance().saveMelodicSequence(testSequencer);
                log.addIndentedLine("Successfully saved test sequence", 1);
            } catch (Exception e) {
                log.addError("Error saving test sequence: " + e.getMessage());
                return log;
            }
            
            // 3. Retrieve and verify
            log.addSection("3. Retrieving and Verifying");
            
            try {
                MelodicSequenceData retrievedSequence = RedisService.getInstance()
                        .findMelodicSequenceById(testSequence.getId(), testSequencerId);
                
                if (retrievedSequence == null) {
                    log.addError("Failed to retrieve test sequence");
                    return log;
                }
                
                log.addIndentedLine("Successfully retrieved sequence: " + retrievedSequence.getName(), 1);
                
                // Verify basic properties
                boolean nameMatches = "Diagnostic Test Sequence".equals(retrievedSequence.getName());
                log.addIndentedLine("Name matches: " + nameMatches, 1);
                
                if (!nameMatches) {
                    log.addWarning("Name does not match: " + retrievedSequence.getName());
                }
                
                // Verify pattern
                boolean patternMatches = true;
                for (int i = 0; i < 16; i++) {
                    if (retrievedSequence.isStepActive(i) != (i % 4 == 0)) {
                        patternMatches = false;
                        log.addWarning("Step " + i + " active state does not match");
                    }
                    
                    if (retrievedSequence.getNoteValue(i) != (60 + (i % 12))) {
                        patternMatches = false;
                        log.addWarning("Step " + i + " note value does not match");
                    }
                }
                
                log.addIndentedLine("Pattern matches: " + patternMatches, 1);
                
            } catch (Exception e) {
                log.addError("Error retrieving test sequence: " + e.getMessage());
            }
            
            // 4. Apply to sequencer
            log.addSection("4. Applying to Sequencer");
            
            try {
                boolean wasPlaying = testSequencer.isPlaying();
                if (wasPlaying) {
                    testSequencer.stop();
                }
                
                RedisService.getInstance().applyMelodicSequenceToSequencer(testSequence, testSequencer);
                log.addIndentedLine("Applied test sequence to sequencer", 1);
                
                // Verify sequence was applied correctly
                MelodicSequenceData appliedData = testSequencer.getSequenceData();
                if (appliedData != null && appliedData.getId().equals(testSequence.getId())) {
                    log.addIndentedLine("Sequence ID verification passed", 1);
                } else {
                    log.addError("Sequence ID verification failed");
                }
                
                // Verify pattern was applied
                boolean patternApplied = true;
                for (int i = 0; i < 16; i++) {
                    if (testSequencer.getSequenceData().isStepActive(i) != (i % 4 == 0)) {
                        patternApplied = false;
                        log.addWarning("Step " + i + " active state was not applied correctly");
                    }
                }
                
                log.addIndentedLine("Pattern application: " + patternApplied, 1);
                
                // Restore playing state
                if (wasPlaying) {
                    testSequencer.start();
                }
                
            } catch (Exception e) {
                log.addError("Error applying test sequence: " + e.getMessage());
            }
            
            // 5. Clean up
            log.addSection("5. Clean Up");
            
            try {
                RedisService.getInstance().deleteMelodicSequence(testSequencerId, testSequence.getId());
                log.addIndentedLine("Deleted test sequence", 1);
            } catch (Exception e) {
                log.addWarning("Error cleaning up test sequence: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}
