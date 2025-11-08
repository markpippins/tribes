package com.angrysurfer.core;

import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

/**
 * Headless smoke test that runs the minimal initialize() sequence used at startup.
 * This ensures that managers can be initialized deterministically in a headless CI environment
 * (no GUI). The test is permissive: it fails only if an unexpected exception is thrown.
 */
class HeadlessStartupTest {

    private static final Logger logger = LoggerFactory.getLogger(HeadlessStartupTest.class);
    @Test
    void testInitializeSequenceHeadless() {
        try {
            // DeviceManager: probes MIDI devices but handles missing devices gracefully
            DeviceManager.getInstance().initialize();

            // Internal synth / soundbank
            InternalSynthManager.getInstance().initialize();

            // Instrument manager (may read cache)
            InstrumentManager.getInstance().initialize();

            // Session manager: should prepare session structures (uses Redis if present)
            SessionManager.getInstance().initialize();

            // PlayerManager: register handlers (should be safe headless)
            PlayerManager.getInstance().initialize();

        } catch (Exception e) {
            // Make the failure clear in CI logs
            logger.error("Headless initialize sequence failed", e);
            fail("Headless initialize sequence failed: " + e.getMessage());
        }
    }
}
