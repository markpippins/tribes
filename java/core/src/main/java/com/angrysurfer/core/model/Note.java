package com.angrysurfer.core.model;

import com.angrysurfer.core.sequencer.TimingUpdate;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Note extends Player {
    // Note specific properties
    private String name = "Note";

    /**
     * Default constructor for JPA
     */
    public Note() {
        setMelodicPlayer(true);
        setFollowSessionOffset(false);
    }

    /**
     * Main constructor for Note with basic parameters
     */
    public Note(String name, Session session, InstrumentWrapper instrument, int note,
                List<Integer> allowedControlMessages) {
        initialize(name, session, instrument, allowedControlMessages);
        setRootNote(note);
        setMelodicPlayer(true);
        setFollowSessionOffset(false);
    }

    /**
     * Extended constructor with velocity parameters
     */
    public Note(String name, Session session, InstrumentWrapper instrument, int note,
                List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        initialize(name, session, instrument, allowableControlMessages);
        setRootNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
        setMelodicPlayer(true);
        setFollowSessionOffset(false);
    }

    /**
     * Simplified constructor for quick note creation
     */
    // public Note(String name, int rootNote) {
    // initialize();
    // this.name = name;
    // setRootNote(rootNote);
    // }
    @Override
    public void onTick(TimingUpdate timingUpdate) {
        // Implementation details
    }
}
