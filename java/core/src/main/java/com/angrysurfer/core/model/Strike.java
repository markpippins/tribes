package com.angrysurfer.core.model;

import java.util.HashSet;
import java.util.List;

import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Strike extends Player {

    /**
     * Default constructor - lightweight, no side-effects
     */
    public Strike() {
        setRules(new HashSet<>());
        setDrumPlayer(true);
        setFollowSessionOffset(true);
    }

    /**
     * Parameterized constructor - still lightweight; callers must invoke initialize(...)
     */
    public Strike(String name, Session session, InstrumentWrapper instrument, int note,
                  List<Integer> allowedControlMessages) {
        setName(name);
        setSession(session);
        setInstrument(instrument);
        setAllowedControlMessages(allowedControlMessages);
        setRootNote(note);
        setDrumPlayer(true);
        setFollowSessionOffset(true);
    }

    @Override
    public Integer getDefaultChannel() {
        return SequencerConstants.MIDI_DRUM_CHANNEL;
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        boolean followRules = Boolean.TRUE.equals(getFollowRules());

        if (!followRules || shouldPlay(timingUpdate)) {
            try {
                int noteToPlay = getRootNote();
                if (Boolean.TRUE.equals(getFollowSessionOffset())) {
                    Session s = SessionManager.getInstance().getActiveSession();
                    if (s != null) noteToPlay += s.getNoteOffset();
                }
                noteOn(noteToPlay, getLevel());
            } catch (Exception e) {
                logger.error("Error in Strike.onTick: {}", e.getMessage(), e);
            }
        }
    }

    public Object[] toRow() {
        logger.debug("Converting Strike to row - ID: {}, Name: {}", getId(), getName());
        return new Object[]{
                getName(),
                getChannel(),
                getSwing(),
                getLevel(),
                getRootNote(),
                getMinVelocity(),
                getMaxVelocity(),
                getPreset(),
                getStickyPreset(),
                getProbability(),
                getRandomDegree(),
                getRatchetCount(),
                getRatchetInterval(),
                getUseInternalBeats(),
                getUseInternalBars(),
                getPanPosition(),
                getPreserveOnPurge(),
                getSparse()
        };
    }

}
