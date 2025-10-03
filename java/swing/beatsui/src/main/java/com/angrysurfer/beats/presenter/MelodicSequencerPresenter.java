package com.angrysurfer.beats.presenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.panel.sequencer.mono.MelodicSequencerPanel;
import com.angrysurfer.beats.panel.session.SessionControlPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.MelodicScaleSelectionEvent;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.sequencer.MelodicSequencer;

public class MelodicSequencerPresenter implements IBusListener {
    private final MelodicSequencer model;
    private final MelodicSequencerPanel view;

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerPresenter.class);
    
    public MelodicSequencerPresenter(MelodicSequencer model, MelodicSequencerPanel view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void onAction(Command action) {
        System.out.println("MelodicSequencerPresenter.onAction: " + action.getCommand());
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            // Arrow syntax for all cases
            case Commands.MELODIC_SEQUENCE_LOADED,
                 Commands.MELODIC_SEQUENCE_CREATED,
                 Commands.MELODIC_SEQUENCE_SELECTED,
                 Commands.MELODIC_SEQUENCE_UPDATED -> {
                // Check if this event applies to our sequencer
                if (action.getData() instanceof MelodicSequencerEvent event) {
                    if (event.getSequencerId().equals(model.getId())) {
                        logger.info("Updating UI for sequence event: {}", action.getCommand());
                        view.syncUIWithSequencer();
                    }
                } else {
                    // If no specific sequencer event data, update anyway
                    view.syncUIWithSequencer();
                }
            }
            case Commands.ROOT_NOTE_SELECTED -> {
                model.getSequenceData().setRootNoteFromString((String) action.getData());
                model.getPlayer().setRootNote(model.getSequenceData().getRootNote());
                view.syncUIWithSequencer();
            }

            case Commands.SCALE_SELECTED -> {
                // Only update if this event is for our specific sequencer or from the global
                // controller
                if (action.getData() instanceof MelodicScaleSelectionEvent(Integer sequencerId, String scale)) {
                    // Check if this event is for our sequencer
                    if (sequencerId != null && sequencerId.equals(model.getId())) {
                        // Update the scale in the sequencer
                        model.getSequenceData().setScale(scale);

                        // Update the UI without publishing new events
                        if (view.getScalePanel() != null) {
                            view.getScalePanel().setSelectedScale(scale);
                        }

                        // Log the specific change
                        logger.debug("Set scale to {} for sequencer {}", scale, model.getId());
                    }
                }
                // Handle global scale changes from session panel (separate implementation)
                else if (action.getData() instanceof String scale &&
                        (action.getSender() instanceof SessionControlPanel)) {
                    // This is a global scale change from the session panel
                    model.getSequenceData().setScale(scale);

                    // Update UI without publishing new events
                    if (view.getScalePanel() != null)
                        view.getScalePanel().setSelectedScale(scale);

                    logger.debug("Set scale to {} from global session change", scale);
                }
            }

            case Commands.PATTERN_UPDATED -> {
                // Your existing PATTERN_UPDATED handler code
                // Only handle events from our sequencer to avoid loops
                if (action.getSender() == model) {
                    view.syncUIWithSequencer();
                }
            }

            case Commands.INSTRUMENT_CHANGED -> {
                // Check if this update is for our sequencer's player
                // if (action.getData() instanceof Player &&
                // model != null &&
                // model.getPlayer() != null &&
                // model.getPlayer().getId().equals(((Player) action.getData()).getId())) {

                // SwingUtilities.invokeLater(this::updateInstrumentInfoLabel);
                // }
            }

            default -> {
                // Optional default case
            }
        }
    }
}
