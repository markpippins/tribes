package com.angrysurfer.beats.presenter;

import java.util.List;

import javax.swing.SwingUtilities;

import com.angrysurfer.beats.panel.sequencer.poly.DrumSequencerGridPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.sequencer.DrumSequenceModifier;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.DrumSequencerManager;

public class DrumSequencerPresenter implements IBusListener {
    private final DrumSequencer model;
    private final DrumSequencerGridPanel view;

    public DrumSequencerPresenter(DrumSequencer model, DrumSequencerGridPanel view) {
        this.model = model;
        this.view = view;
        // Add listeners to the model and view
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_UPDATED -> {
                // Update the UI to reflect new sequence data
                SwingUtilities.invokeLater(() -> {
                    // Update the entire grid UI
                    view.getGridPanel().refreshGridUI();

                    // Update the parameter controls for the selected drum
                    view.updateParameterControls();

                    // Update the drum info panel
                    if (view.getDrumInfoPanel() != null) {
                        view.getDrumInfoPanel().updateInfo(DrumSequencerManager.getInstance().getSelectedPadIndex());
                    }

                    // Reset all step highlighting
                    view.getGridPanel().clearAllStepHighlighting();
                });
            }

            case Commands.DRUM_PAD_SELECTED -> {
                if (action.getData() instanceof DrumPadSelectionEvent event) {
                    view.selectDrumPad(event.newSelection());
                }
            }

            case Commands.TRANSPORT_START -> {
                // Show step highlighting when playing starts
                view.setPlaying(true);
                if (view.getGridPanel() != null) {
                    view.getGridPanel().setPlayingState(true);
                }
            }

            case Commands.TRANSPORT_STOP -> {
                // Hide step highlighting when playing stops
                view.setPlaying(false);
                if (view.getGridPanel() != null) {
                    view.getGridPanel().setPlayingState(false);
                }
            }

            case Commands.MAX_LENGTH_SELECTED -> {
                if (action.getData() instanceof Integer maxLength) {
                    List<Integer> updatedDrums = DrumSequenceModifier.applyMaxPatternLength(model, maxLength);

                    // Update UI for affected drums
                    for (int drumIndex : updatedDrums) {
                        view.getGridPanel().updateStepButtonsForDrum(drumIndex);
                    }

                    // Update parameter controls if the selected drum was affected
                    if (updatedDrums.contains(DrumSequencerManager.getInstance().getSelectedPadIndex())) {
                        view.updateParameterControls();
                    }

                    // Show confirmation message
                    view.showPatternLengthUpdateMessage(updatedDrums.size());
                }
            }

            case Commands.EUCLIDEAN_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    boolean[] pattern = (boolean[]) result[1];

                    // Use the static method from DrumSequenceModifier
                    boolean success = DrumSequenceModifier.applyEuclideanPattern(model, drumIndex, pattern);

                    // If successful, update the UI
                    if (success) {
                        view.getGridPanel().updateStepButtonsForDrum(drumIndex);
                        view.updateParameterControls();
                    }
                }
            }

            case Commands.FILL_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    int startStep = (Integer) result[1];
                    String fillType = (String) result[2];

                    // Apply the fill pattern
                    int patternLength = model.getPatternLength(drumIndex);

                    for (int i = startStep; i < patternLength; i++) {
                        boolean shouldActivate = false;

                        switch (fillType) {
                            case "all" -> shouldActivate = true;
                            case "everyOther" -> shouldActivate = ((i - startStep) % 2) == 0;
                            case "every4th" -> shouldActivate = ((i - startStep) % 4) == 0;
                            case "decay" -> {
                                shouldActivate = true;
                                model.setVelocity(drumIndex,
                                        Math.max(SequencerConstants.DEFAULT_VELOCITY / 2, SequencerConstants.DEFAULT_VELOCITY - ((i - startStep) * 8)));
                            }
                        }

                        if (shouldActivate) {
                            model.toggleStep(drumIndex, i);
                        }
                    }

                    // Update UI to reflect changes
                    view.getGridPanel().updateStepButtonsForDrum(drumIndex);
                }
            }

            case Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED -> {
                if (action.getData() instanceof Integer drumIndex) {
                    // Update step buttons for the specified drum
                    SwingUtilities.invokeLater(() -> {
                        view.updateStepButtonsForDrum(drumIndex);
                    });
                }
            }

            case Commands.DRUM_GRID_REFRESH_REQUESTED -> {
                // Refresh the entire grid UI
                SwingUtilities.invokeLater(() -> {
                    view.refreshGridUI();
                });
            }

            case Commands.DRUM_PATTERN_CLEAR_REQUESTED -> {
                if (action.getData() instanceof Integer drumIndex) {
                    // Clear the pattern for the specified drum
                    SwingUtilities.invokeLater(() -> {
                        view.clearRow(drumIndex);
                    });
                }
            }
        }
    }
}
