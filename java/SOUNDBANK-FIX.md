# Soundbank Fix and Refactoring Plan

This document outlines the steps taken to refactor the BeatGeneratorApp and fix the soundbank loading issues.

## Problems Identified

*   **Overwrought MIDI/Sound Plumbing:** The MIDI and sound logic is overly complex and difficult to understand.
*   **UI Slowness:** The Swing UI is slow and unresponsive at times.
*   **Tight Coupling:** The UI and core are tightly coupled, making the code difficult to test and maintain.
*   **God Objects:** The `InternalSynthManager` class is a god object that has too many responsibilities.
*   **Inconsistent Timing:** The `DrumSequencer` and `MelodicSequencer` use different timing mechanisms, which can lead to synchronization issues.

## Refactoring Steps Taken

1.  **Consolidated Schedulers:** The three different schedulers for note-off events have been consolidated into a single, shared scheduler in the `InstrumentWrapper` class.
2.  **Refactored `InternalSynthManager`:** The `InternalSynthManager` has been refactored to use a `SynthProvider` to provide a `Synthesizer` instance and a `NotePlayer` to play notes. This has reduced the complexity of the `InternalSynthManager` and made the code more modular.
3.  **Created `SoundbankLoader`:** A new `SoundbankLoader` class has been created to handle the soundbank loading logic. This has further reduced the responsibilities of the `InternalSynthManager`.

## Next Steps

1.  **Decouple UI from Core:** Refactor the `DrumSequencerGridPanel` and `MelodicSequencerPanel` to use a Model-View-Presenter (MVP) or Model-View-ViewModel (MVVM) architecture.
2.  **Use a more efficient way to update the UI:** Replace the command bus-based UI updates with a more efficient mechanism, such as observing the state of the sequencers directly.
3.  **Optimize UI rendering:** Profile the UI code to identify and optimize any performance bottlenecks.
4.  **Address inconsistent timing:** Refactor the `DrumSequencer` and `MelodicSequencer` to use a consistent timing mechanism.
