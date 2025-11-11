package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.ChannelManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic helper for ChannelManager
 */
public class ChannelManagerDiagnostics {

    /**
     * Run diagnostics on the ChannelManager
     */
    public DiagnosticLogBuilder testChannelManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("ChannelManager Diagnostics");

        try {
            ChannelManager manager = ChannelManager.getInstance();
            log.addLine("ChannelManager instance obtained: " + (manager != null));

            // Test channel status
            log.addSection("Channel Availability Status");
            boolean[] channelStatus = new boolean[16];
            List<Integer> inUseChannels = new ArrayList<>();
            List<Integer> availableChannels = new ArrayList<>();

            // Check each channel
            for (int i = 0; i < 16; i++) {
                channelStatus[i] = manager.isChannelInUse(i);
                if (channelStatus[i]) {
                    inUseChannels.add(i);
                } else {
                    availableChannels.add(i);
                }
            }

            log.addIndentedLine("Channels in use: " + inUseChannels.size(), 1);
            for (Integer channel : inUseChannels) {
                log.addIndentedLine("Channel " + channel + (channel == SequencerConstants.MIDI_DRUM_CHANNEL ? " (Drums)" : ""), 2);
            }

            log.addIndentedLine("Available channels: " + availableChannels.size(), 1);
            for (Integer channel : availableChannels) {
                log.addIndentedLine("Channel " + channel, 2);
            }

            // Test channel assignment
            log.addSection("Channel Assignment Test");

            // Get next available melodic channel
            int nextChannel = manager.getNextAvailableMelodicChannel();
            log.addLine("Next available melodic channel: " + nextChannel);

            // Release the channel we just got
            manager.releaseChannel(nextChannel);
            log.addLine("Released channel: " + nextChannel);

            // Check if it's available again
            boolean isAvailableAgain = !manager.isChannelInUse(nextChannel);
            log.addLine("Channel " + nextChannel + " is available again: " + isAvailableAgain);

            if (!isAvailableAgain) {
                log.addWarning("Channel " + nextChannel + " was not properly released");
            }

            // Test sequencer channel mapping
            log.addSection("Sequencer Channel Mapping");
            for (int i = 0; i < 8; i++) {
                int channel = manager.getChannelForSequencerIndex(i);
                log.addIndentedLine("Sequencer " + i + " → Channel " + channel, 1);

                // Release this channel when done
                manager.releaseChannel(channel);
            }

            // Test conflict resolution
            log.addSection("Channel Conflict Resolution");

            // Try to reserve the drum channel
            boolean reservedDrumChannel = manager.reserveChannel(9);
            log.addLine("Reserved drum channel (9): " + reservedDrumChannel);

            if (reservedDrumChannel) {
                log.addWarning("Drum channel 9 was allowed to be reserved, which should not happen");
            }

            // Test reservation
            int channelToReserve = 5;
            boolean reserved = manager.reserveChannel(channelToReserve);
            log.addLine("Reserved channel " + channelToReserve + ": " + reserved);

            // Try to reserve it again
            boolean reservedAgain = manager.reserveChannel(channelToReserve);
            log.addLine("Reserved channel " + channelToReserve + " again: " + reservedAgain);

            if (reservedAgain) {
                log.addWarning("Channel " + channelToReserve + " was allowed to be reserved twice");
            }

            // Release the channel
            manager.releaseChannel(channelToReserve);

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }
}
