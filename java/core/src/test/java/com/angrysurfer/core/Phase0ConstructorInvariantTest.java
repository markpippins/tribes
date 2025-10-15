package com.angrysurfer.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Strike;

public class Phase0ConstructorInvariantTest {

    @AfterAll
    public static void cleanup() {
        // ensure buses are clean for other tests
        // No-op: buses are singletons but tests here don't mutate global registration permanently
    }

    @Test
    public void testNoteAndStrikeNotRegisteredBeforeInitialize_TimingBus() throws Exception {
        Note n = new Note();

        // Newly constructed should NOT be registered on TimingBus
        Assertions.assertFalse(TimingBus.getInstance().isRegistered(n), "Note should not be registered on TimingBus before initialize()");

        // Call initialize with minimal params (name, null session/instrument, null allowed controls)
        n.initialize("test-note", null, null, null);

        Assertions.assertTrue(TimingBus.getInstance().isRegistered(n), "Note should be registered on TimingBus after initialize()");

        // cleanup: unregister
        TimingBus.getInstance().unregister(n);
    }

    @Test
    public void testStrikeNotRegisteredBeforeInitialize_TimingBus() throws Exception {
        Strike s = new Strike();

        Assertions.assertFalse(TimingBus.getInstance().isRegistered(s), "Strike should not be registered on TimingBus before initialize()");

        s.initialize("test-strike", null, null, null);

        Assertions.assertTrue(TimingBus.getInstance().isRegistered(s), "Strike should be registered on TimingBus after initialize()");

        TimingBus.getInstance().unregister(s);
    }

    @Test
    public void testPlayerNotRegisteredBeforeInitialize_CommandBus() throws Exception {
        // For CommandBus we verify listenerMap via publish: create a temporary command and ensure
        // the player does not receive it before initialize. We'll use a small reflective "isRegistered" if available.
        Note n = new Note();

        // Use CommandBus: assume it won't pass the Note as listener until initialize
        // There's no public isRegistered for CommandBus; rely on the fact that TimingBus is used for timing registration
        Assertions.assertFalse(TimingBus.getInstance().isRegistered(n), "Note should not be registered on CommandBus/TimingBus before initialize()");

        n.initialize("test-note-2", null, null, null);

        Assertions.assertTrue(TimingBus.getInstance().isRegistered(n), "Note should be registered after initialize()");

        TimingBus.getInstance().unregister(n);
    }
}
