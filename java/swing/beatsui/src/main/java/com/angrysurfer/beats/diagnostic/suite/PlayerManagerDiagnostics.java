package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.PlayerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Diagnostic helper for PlayerManager
 */
public class PlayerManagerDiagnostics {

    /**
     * Run diagnostics on PlayerManager
     */
    public DiagnosticLogBuilder testPlayerManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("PlayerManager Diagnostics");

        try {
            PlayerManager manager = PlayerManager.getInstance();
            log.addLine("PlayerManager instance obtained: " + (manager != null));

            // Test cache state
            log.addSection("Player Cache Status");
            Map<Long, Player> playerCache = manager.getPlayerCache();
            log.addLine("Player cache size: " + playerCache.size());

            // No longer use active player - it's been replaced by the event system
            log.addLine("Note: Active player concept has been replaced by PlayerSelectionEvent system");

            // Test channel assignment
            log.addSection("Player Channel Assignment");

            Map<Integer, AtomicInteger> channelCount = new HashMap<>();
            List<String> channelErrors = new ArrayList<>();

            // Check channel assignments
            for (Player player : playerCache.values()) {
                Integer channel = player.getChannel();

                if (channel == null) {
                    channelErrors.add("Player " + player.getId() + " has no channel assigned");
                    continue;
                }

                // Count players per channel
                channelCount.computeIfAbsent(channel, c -> new AtomicInteger()).incrementAndGet();

                // Validate channel for player type
                if (player.isDrumPlayer() && channel != SequencerConstants.MIDI_DRUM_CHANNEL) {
                    channelErrors.add("Drum player " + player.getId() + " using non-drum channel: " + channel);
                } else if (!player.isDrumPlayer() && channel == SequencerConstants.MIDI_DRUM_CHANNEL) {
                    channelErrors.add("Melodic player " + player.getId() + " using drum channel 9");
                }
            }

            // Report channel distribution
            for (Map.Entry<Integer, AtomicInteger> entry : channelCount.entrySet()) {
                log.addIndentedLine("Channel " + entry.getKey() + ": " + entry.getValue() + " players", 1);
            }

            // Report channel errors
            if (!channelErrors.isEmpty()) {
                log.addWarning("Found " + channelErrors.size() + " channel assignment issues:");
                channelErrors.forEach(err -> log.addIndentedLine(err, 1));
            }

            // Test instrument references
            log.addSection("Player-Instrument Consistency");
            List<String> instrumentErrors = new ArrayList<>();

            for (Player player : playerCache.values()) {
                // Check instrument ID consistency
                Long instId = player.getInstrumentId();
                InstrumentWrapper instrument = player.getInstrument();

                if (instId == null) {
                    instrumentErrors.add("Player " + player.getId() + " has no instrument ID");
                } else if (instrument == null) {
                    instrumentErrors.add("Player " + player.getId() + " has instrument ID but no instrument object");
                } else if (!instId.equals(instrument.getId())) {
                    instrumentErrors.add("Player " + player.getId() + " has mismatched instrument ID (" +
                            instId + " vs " + instrument.getId() + ")");
                }

                // Check channel alignment
                if (instrument != null && player.getChannel() != null &&
                        instrument.getChannel() != null &&
                        !player.getChannel().equals(instrument.getChannel())) {
                    instrumentErrors.add("Player " + player.getId() + " has channel " + player.getChannel() +
                            " but its instrument has channel " + instrument.getChannel());
                }
            }

            // Report instrument errors
            if (!instrumentErrors.isEmpty()) {
                log.addWarning("Found " + instrumentErrors.size() + " instrument reference issues:");
                instrumentErrors.forEach(err -> log.addIndentedLine(err, 1));
            }

            // Test getPlayerById functionality
            log.addSection("Player Retrieval Test");

            if (!playerCache.isEmpty()) {
                // Test with first cached player
                Long testId = playerCache.keySet().iterator().next();
                log.addLine("Testing getPlayerById with cached player ID: " + testId);
                Player retrieved = manager.getPlayerById(testId);
                log.addLine("Retrieved player: " + (retrieved != null ? "Success" : "Failed"));
                if (retrieved != null) {
                    log.addLine("Player name: " + retrieved.getName());
                }
            } else {
                log.addWarning("No players available to test getPlayerById");
            }

            // Test rule management
            log.addSection("Rule Management");
            int playersWithRules = 0;
            int totalRules = 0;

            for (Player player : playerCache.values()) {
                if (player.getRules() != null && !player.getRules().isEmpty()) {
                    playersWithRules++;
                    totalRules += player.getRules().size();
                }
            }

            log.addLine("Players with rules: " + playersWithRules);
            log.addLine("Total rules: " + totalRules);

            // Test command bus registration
            log.addSection("Event System Registration");
            CommandBus commandBus = CommandBus.getInstance();
            log.addLine("CommandBus instance obtained: " + (commandBus != null));

            // Check channel consistency
            log.addSection("Channel Consistency Test");
            log.addLine("Running ensureChannelConsistency...");

            try {
                manager.ensureChannelConsistency();
                log.addLine("Channel consistency check completed successfully");

                // Verify if drum players are on channel 9
                boolean allDrumPlayersOnChannel9 = true;
                for (Player player : playerCache.values()) {
                    if (player.isDrumPlayer() && (player.getChannel() == null || player.getChannel() != SequencerConstants.MIDI_DRUM_CHANNEL)) {
                        allDrumPlayersOnChannel9 = false;
                        log.addWarning("Drum player " + player.getId() + " not on channel 9 after consistency check");
                    }
                }

                if (allDrumPlayersOnChannel9) {
                    log.addLine("All drum players correctly assigned to channel 9");
                }

                // Count players per channel after consistency check
                Map<Integer, AtomicInteger> postCheckChannelCount = new HashMap<>();
                for (Player player : playerCache.values()) {
                    Integer channel = player.getChannel();
                    if (channel != null) {
                        postCheckChannelCount.computeIfAbsent(channel, c -> new AtomicInteger()).incrementAndGet();
                    }
                }

                log.addLine("Channel distribution after consistency check:");
                for (Map.Entry<Integer, AtomicInteger> entry : postCheckChannelCount.entrySet()) {
                    log.addIndentedLine("Channel " + entry.getKey() + ": " + entry.getValue() + " players", 1);
                }
            } catch (Exception e) {
                log.addError("Failed to run channel consistency check: " + e.getMessage());
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Test a specific player's instrument and channel configuration
     */
    public DiagnosticLogBuilder testSpecificPlayer(Long playerId) {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Player Diagnostics - ID: " + playerId);

        try {
            PlayerManager manager = PlayerManager.getInstance();
            Player player = manager.getPlayerById(playerId);

            if (player == null) {
                log.addError("Player not found with ID: " + playerId);
                return log;
            }

            log.addLine("Found player: " + player.getName());
            log.addLine("Type: " + (player.isDrumPlayer() ? "Drum" : "Melodic"));
            log.addLine("Channel: " + player.getChannel());
            log.addLine("MIDI level: " + player.getLevel());
            log.addLine("Root note: " + player.getRootNote());

            // Instrument details
            log.addSection("Instrument Details");
            InstrumentWrapper instrument = player.getInstrument();

            if (instrument == null) {
                log.addWarning("No instrument assigned to player");
                log.addLine("Instrument ID: " + player.getInstrumentId());
            } else {
                log.addLine("Instrument name: " + instrument.getName());
                log.addLine("Instrument ID: " + instrument.getId());
                log.addLine("Instrument channel: " + instrument.getChannel());
                log.addLine("Bank: " + instrument.getBankIndex());
                log.addLine("Preset: " + instrument.getPreset());
                log.addLine("Device: " + (instrument.getDeviceName() != null ? instrument.getDeviceName() : "None"));

                // Verify consistency
                if (!player.getInstrumentId().equals(instrument.getId())) {
                    log.addError("Instrument ID mismatch: player references " + player.getInstrumentId() +
                            " but instrument has ID " + instrument.getId());
                }

                if (player.getChannel() != null && instrument.getChannel() != null &&
                        !player.getChannel().equals(instrument.getChannel())) {
                    log.addWarning("Channel mismatch: player uses channel " + player.getChannel() +
                            " but instrument uses channel " + instrument.getChannel());
                }
            }

            // Rule details
            log.addSection("Rules");
            if (player.getRules() != null && !player.getRules().isEmpty()) {
                log.addLine("Player has " + player.getRules().size() + " rules:");
                int index = 1;
                for (Rule rule : player.getRules()) {
                    log.addIndentedLine("Rule " + index + ":", 1);
                    log.addIndentedLine("ID: " + rule.getId(), 2);
                    log.addIndentedLine("Part: " + rule.getPart(), 2);
                    log.addIndentedLine("Operator: " + rule.getOperator(), 2);
                    log.addIndentedLine("Comparison: " + rule.getComparison(), 2);
                    log.addIndentedLine("Value: " + rule.getValue(), 2);
                    index++;
                }
            } else {
                log.addLine("Player has no rules");
            }

            // Test applying preset
            log.addSection("Preset Application Test");
            if (player.getInstrument() != null && player.getInstrument().getPreset() != null) {
                try {
                    manager.applyInstrumentPreset(player);
                    log.addLine("Successfully applied preset: " + player.getInstrument().getPreset());
                } catch (Exception e) {
                    log.addError("Failed to apply preset: " + e.getMessage());
                }
            } else {
                log.addWarning("No preset available to test");
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }
}