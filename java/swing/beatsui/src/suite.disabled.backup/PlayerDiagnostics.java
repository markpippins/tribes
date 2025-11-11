package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.PlayerHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.SequencerConstants;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for player diagnostics
 */
public class PlayerDiagnostics {

    private final RedisService redisService;
    private final PlayerHelper playerHelper;
    private final InstrumentHelper instrumentHelper;

    /**
     * Constructor
     */
    public PlayerDiagnostics() {
        this.redisService = RedisService.getInstance();
        this.playerHelper = redisService.getPlayerHelper();
        this.instrumentHelper = redisService.getInstrumentHelper();
    }

    /**
     * Tests player and instrument integrity in the database
     */
    public DiagnosticLogBuilder testPlayerInstrumentIntegrity() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Player/Instrument Integrity Test");

        try {
            // Test 1: List all players and check their instruments
            log.addSection("1. Checking all players");
            Long[] playerIds = playerHelper.getCachedPlayerIds();

            if (playerIds == null) {
                log.addError("Player ID list is null");
                return log;
            }

            log.addIndentedLine("Found " + playerIds.length + " players", 1);

            int playersWithInstruments = 0;
            int playersWithoutInstruments = 0;
            int playersWithInvalidInstruments = 0;

            // Limit check to max 20 players to avoid overwhelming output
            int checkLimit = Math.min(playerIds.length, 20);

            for (int i = 0; i < checkLimit; i++) {
                Long playerId = playerIds[i];
                for (String className : Arrays.asList("Strike", "Note")) {
                    try {
                        Player player = playerHelper.findPlayerById(playerId, className);
                        if (player != null) {
                            log.addIndentedLine("Player " + playerId + " (" + className + "): " +
                                    player.getName(), 1);

                            if (player.getInstrumentId() != null) {
                                InstrumentWrapper instrument =
                                        instrumentHelper.findInstrumentById(player.getInstrumentId());

                                if (instrument != null) {
                                    log.addIndentedLine("Instrument: " + instrument.getName() +
                                            " (ID: " + instrument.getId() + ")", 2);
                                    playersWithInstruments++;
                                } else {
                                    log.addIndentedLine("ERROR: Referenced instrument " +
                                            player.getInstrumentId() + " not found", 2);
                                    playersWithInvalidInstruments++;
                                }
                            } else {
                                log.addIndentedLine("No instrument assigned", 2);
                                playersWithoutInstruments++;
                            }
                        }
                    } catch (Exception e) {
                        // Skip exceptions for player type mismatches
                    }
                }
            }

            log.addIndentedLine("Players with valid instruments: " + playersWithInstruments, 1)
                    .addIndentedLine("Players without instruments: " + playersWithoutInstruments, 1)
                    .addIndentedLine("Players with invalid instruments: " + playersWithInvalidInstruments, 1);

            if (playersWithInvalidInstruments > 0) {
                log.addError(playersWithInvalidInstruments + " players have invalid instrument references");
            }

            // Test 2: List all instruments and check for orphans
            log.addSection("2. Checking all instruments");
            List<InstrumentWrapper> instruments = instrumentHelper.findAllInstruments();
            log.addIndentedLine("Found " + instruments.size() + " instruments", 1);

            int usedInstruments = 0;
            int unusedInstruments = 0;

            List<Long> instrumentIds = instruments.stream()
                    .map(InstrumentWrapper::getId)
                    .collect(Collectors.toList());

            for (Long instrumentId : instrumentIds) {
                InstrumentWrapper instrument = instrumentHelper.findInstrumentById(instrumentId);
                if (instrument != null) {
                    log.addIndentedLine("Instrument " + instrumentId + ": " + instrument.getName(), 1);

                    // Check if any player uses this instrument
                    boolean isUsed = false;
                    for (Long playerId : playerIds) {
                        for (String className : Arrays.asList("Strike", "Note")) {
                            try {
                                Player player = playerHelper.findPlayerById(playerId, className);
                                if (player != null && player.getInstrumentId() != null &&
                                        player.getInstrumentId().equals(instrumentId)) {
                                    log.addIndentedLine("Used by player " + playerId +
                                            " (" + player.getName() + ")", 2);
                                    isUsed = true;
                                    break;
                                }
                            } catch (Exception e) {
                                // Skip exceptions for player type mismatches
                            }
                        }
                        if (isUsed) break;
                    }

                    if (isUsed) {
                        usedInstruments++;
                    } else {
                        log.addIndentedLine("Not used by any player", 2);
                        unusedInstruments++;
                    }
                }
            }

            log.addIndentedLine("Used instruments: " + usedInstruments, 1)
                    .addIndentedLine("Unused instruments: " + unusedInstruments, 1);

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Create a test player, test its operations, and clean up
     */
    public DiagnosticLogBuilder testPlayerOperations() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Player Operations Test");

        try {
            // Test creating a new player
            log.addSection("1. Creating test player");

            // Use the built-in method to create a new Strike player
            Player testPlayer = playerHelper.newStrike();

            // Update its properties for testing
            String testName = "Test Player " + System.currentTimeMillis();
            testPlayer.setName(testName);
            testPlayer.setDefaultChannel(SequencerConstants.MIDI_DRUM_CHANNEL); // drum channel
            testPlayer.setRootNote(36); // kick

            log.addIndentedLine("Created player: " + testPlayer.getName() + " with ID: " + testPlayer.getId(), 1);

            // Save the updated player
            playerHelper.savePlayer(testPlayer);
            log.addIndentedLine("Saved player with updated properties", 1);

            Long playerId = testPlayer.getId();

            // Test retrieving the player
            log.addSection("2. Retrieving test player");

            Player retrievedPlayer = playerHelper.findPlayerById(playerId, testPlayer.getPlayerClassName());

            if (retrievedPlayer != null) {
                log.addIndentedLine("Successfully retrieved player: " + retrievedPlayer.getName(), 1);

                if (!testName.equals(retrievedPlayer.getName())) {
                    log.addError("Retrieved player name does not match: expected='" +
                            testName + "', actual='" + retrievedPlayer.getName() + "'");
                }

                if (testPlayer.getChannel() != retrievedPlayer.getChannel()) {
                    log.addError("Retrieved player channel does not match: expected=" +
                            testPlayer.getChannel() + ", actual=" + retrievedPlayer.getChannel());
                }
            } else {
                log.addError("Failed to retrieve created player");
            }

            // Test updating the player
            log.addSection("3. Updating test player");

            String updatedName = "Updated " + testPlayer.getName();
            testPlayer.setName(updatedName);

            log.addIndentedLine("Updating player name to: " + updatedName, 1);
            playerHelper.savePlayer(testPlayer);

            // Verify update
            retrievedPlayer = playerHelper.findPlayerById(playerId, testPlayer.getPlayerClassName());

            if (retrievedPlayer != null && updatedName.equals(retrievedPlayer.getName())) {
                log.addIndentedLine("Successfully updated player name", 1);
            } else {
                log.addError("Failed to update player name");
            }

            // Clean up - delete test player
            log.addSection("4. Deleting test player");

            log.addIndentedLine("Deleting test player", 1);
            playerHelper.deletePlayer(testPlayer);

            // Verify deletion
            retrievedPlayer = playerHelper.findPlayerById(playerId, testPlayer.getPlayerClassName());

            if (retrievedPlayer == null) {
                log.addIndentedLine("Verified player no longer exists", 1);
            } else {
                log.addError("Player still exists after deletion attempt");
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }
}
