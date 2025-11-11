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
 * Helper class for instrument and player diagnostics
 */
public class InstrumentDiagnostics {

    private final RedisService redisService;
    private final PlayerHelper playerHelper;
    private final InstrumentHelper instrumentHelper;

    /**
     * Constructor
     */
    public InstrumentDiagnostics() {
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
     * Create a test instrument, test its operations, and clean up
     */
    public DiagnosticLogBuilder testInstrumentOperations() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Instrument Operations Test");

        try {
            // Test creating a new instrument
            log.addSection("1. Creating test instrument");

            InstrumentWrapper testInstrument = new InstrumentWrapper();
            testInstrument.setName("Test Instrument " + System.currentTimeMillis());
            testInstrument.setDeviceName("Test Device");
            testInstrument.setChannel(SequencerConstants.MIDI_DRUM_CHANNEL); // drum channel

            log.addIndentedLine("Creating instrument: " + testInstrument.getName(), 1);
            instrumentHelper.saveInstrument(testInstrument);

            // The ID is set by saveInstrument via jedis.incr
            Long instrumentId = testInstrument.getId();

            if (instrumentId != null) {
                log.addIndentedLine("Created instrument with ID: " + instrumentId, 1);
            } else {
                log.addError("Failed to get ID for saved instrument");
                return log;
            }

            // Test retrieving the instrument
            log.addSection("2. Retrieving test instrument");

            InstrumentWrapper retrievedInstrument = instrumentHelper.findInstrumentById(instrumentId);

            if (retrievedInstrument != null) {
                log.addIndentedLine("Successfully retrieved instrument: " + retrievedInstrument.getName(), 1);

                if (!testInstrument.getName().equals(retrievedInstrument.getName())) {
                    log.addError("Retrieved instrument name does not match: expected='" +
                            testInstrument.getName() + "', actual='" + retrievedInstrument.getName() + "'");
                }

                if (testInstrument.getChannel() != retrievedInstrument.getChannel()) {
                    log.addError("Retrieved instrument channel does not match: expected=" +
                            testInstrument.getChannel() + ", actual=" + retrievedInstrument.getChannel());
                }
            } else {
                log.addError("Failed to retrieve created instrument");
            }

            // Test updating the instrument
            log.addSection("3. Updating test instrument");

            String updatedName = "Updated " + testInstrument.getName();
            testInstrument.setName(updatedName);

            log.addIndentedLine("Updating instrument name to: " + updatedName, 1);
            instrumentHelper.saveInstrument(testInstrument);

            // Verify update
            retrievedInstrument = instrumentHelper.findInstrumentById(instrumentId);

            if (retrievedInstrument != null && updatedName.equals(retrievedInstrument.getName())) {
                log.addIndentedLine("Successfully updated instrument name", 1);
            } else {
                log.addError("Failed to update instrument name");
            }

            // Clean up - delete test instrument
            log.addSection("4. Deleting test instrument");

            log.addIndentedLine("Deleting test instrument", 1);
            try {
                instrumentHelper.deleteInstrument(instrumentId);
                log.addIndentedLine("Successfully executed delete operation", 1);
            } catch (Exception e) {
                log.addError("Error deleting instrument: " + e.getMessage());
            }

            // Verify deletion
            retrievedInstrument = instrumentHelper.findInstrumentById(instrumentId);

            if (retrievedInstrument == null) {
                log.addIndentedLine("Verified instrument no longer exists", 1);
            } else {
                log.addError("Instrument still exists after deletion attempt");
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }
}
