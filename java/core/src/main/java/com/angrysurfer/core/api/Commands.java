package com.angrysurfer.core.api;

public class Commands {

    // Add new command constants
    public static final String DRUM_STEP_BUTTONS_UPDATE_REQUESTED = "DRUM_STEP_BUTTONS_UPDATE_REQUESTED";
    public static final String DRUM_GRID_REFRESH_REQUESTED = "DRUM_GRID_REFRESH_REQUESTED";
    public static final String DRUM_PATTERN_CLEAR_REQUESTED = "DRUM_PATTERN_CLEAR_REQUESTED";

    // Transport Commands
    public static final String REWIND = "REWIND";
    public static final String PAUSE = "PAUSE";
    public static final String RECORD = "RECORD";
    public static final String STOP = "STOP";
    public static final String PLAY = "PLAY";
    public static final String FORWARD = "FORWARD";

    // Transport commands
    public static final String TRANSPORT_REWIND = "TRANSPORT_REWIND";
    public static final String TRANSPORT_PAUSE = "TRANSPORT_PAUSE";
    public static final String TRANSPORT_RECORD = "TRANSPORT_RECORD";
    public static final String TRANSPORT_STOP = "TRANSPORT_STOP";
    public static final String TRANSPORT_START = "TRANSPORT_START";
    public static final String TRANSPORT_FORWARD = "TRANSPORT_FORWARD";
    public static final String ALL_NOTES_OFF = "ALL_NOTES_OFF";

    // Transport state commands
    public static final String TRANSPORT_STATE_CHANGED = "TRANSPORT_STATE_CHANGED";
    public static final String TRANSPORT_RECORD_STATE_CHANGED = "TRANSPORT_RECORD_STATE_CHANGED";

    // Timing commands - consolidated
    public static final String TIMING_UPDATE = "TIMING_UPDATE";
    public static final String TIMING_TICK = "TIMING_TICK";
    public static final String TIMING_BEAT = "TIMING_BEAT";
    public static final String TIMING_BAR = "TIMING_BAR";
    public static final String TIMING_PART = "TIMING_PART";
    public static final String TIMING_RESET = "TIMING_RESET";
    public static final String TIMING_PARAMETERS_CHANGED = "TIMING_PARAMETERS_CHANGED";

    // File Commands
    public static final String NEW_FILE = "NEW_FILE";
    public static final String OPEN_FILE = "OPEN_FILE";
    public static final String SAVE_FILE = "SAVE_FILE";
    public static final String SAVE_AS = "SAVE_AS";
    public static final String EXIT = "EXIT";

    // Edit Commands
    public static final String CUT = "CUT";
    public static final String COPY = "COPY";
    public static final String PASTE = "PASTE";

    // Theme Commands
    public static final String CHANGE_THEME = "CHANGE_THEME";

    // Player selection commands
    public static final String PLAYER = "PLAYER";

    public static final String PLAYER_ADDED = "PLAYER_ADDED";
    public static final String PLAYER_DELETED = "PLAYER_DELETED";

    // Player CRUD commands
    public static final String PLAYER_ADD_REQUEST = "PLAYER_ADD_REQUEST";
    public static final String PLAYER_EDIT_REQUEST = "PLAYER_EDIT_REQUEST";
    public static final String PLAYER_DELETE_REQUEST = "PLAYER_DELETE_REQUEST";
    public static final String PLAYER_EDIT_CANCELLED = "PLAYER_EDIT_CANCELLED";
    public static final String EDIT_PLAYER_PARAMETERS = "EDIT_PLAYER_PARAMETERS";
    public static final String PLAYER_COPY_EDIT_REQUEST = "PLAYER_COPY_EDIT_REQUEST";

    // Player navigation commands
    public static final String PLAYER_ROW_INDEX_REQUEST = "PLAYER_ROW_INDEX_REQUEST";
    public static final String PLAYER_ROW_INDEX_RESPONSE = "PLAYER_ROW_INDEX_RESPONSE";

    // Rule selection commands
    public static final String RULE = "RULE";

    public static final String RULE_SELECTED = "RULE_SELECTED";
    public static final String RULE_UNSELECTED = "RULE_UNSELECTED";
    public static final String RULE_ADDED = "RULE_ADDED";

    public static final String RULE_EDITED = "RULE_EDITED";
    // Rule CRUD commands
    public static final String RULE_ADD_REQUEST = "RULE_ADD_REQUEST";
    public static final String RULE_EDIT_REQUEST = "RULE_EDIT_REQUEST";
    public static final String RULE_DELETE_REQUEST = "RULE_DELETE_REQUEST";
    public static final String RULE_UPDATED = "RULE_UPDATED";

    // Additional Rule events
    public static final String RULE_LOADED = "RULE_LOADED";
    public static final String RULES_CLEARED = "RULES_CLEARED";

    // Rule editor commands
    public static final String SHOW_RULE_EDITOR = "SHOW_RULE_EDITOR";
    public static final String SHOW_RULE_EDITOR_OK = "SHOW_RULE_EDITOR_OK";
    public static final String SHOW_RULE_EDITOR_CANCEL = "SHOW_RULE_EDITOR_CANCEL";

    // Control code and caption commands
    public static final String CONTROL_CODE_SELECTED = "CONTROL_CODE_SELECTED";
    public static final String CONTROL_CODE_UNSELECTED = "CONTROL_CODE_UNSELECTED";
    public static final String CONTROL_CODE_ADDED = "CONTROL_CODE_ADDED";
    public static final String CONTROL_CODE_UPDATED = "CONTROL_CODE_UPDATED";
    public static final String CONTROL_CODE_DELETED = "CONTROL_CODE_DELETED";
    public static final String CAPTION_ADDED = "CAPTION_ADDED";
    public static final String CAPTION_UPDATED = "CAPTION_UPDATED";
    public static final String CAPTION_DELETED = "CAPTION_DELETED";

    // Control sending commands
    public static final String SEND_ALL_CONTROLS = "SEND_ALL_CONTROLS";
    public static final String SAVE_CONFIG = "SAVE_INSTRUMENT_CONFIG";

    // Session selection commands
    public static final String SESSION = "SESSION";
    public static final String SESSION_SELECTED = "SESSION_SELECTED";
    public static final String SESSION_UPDATED = "SESSION_UPDATED";

    // Session state commands
    public static final String SESSION_UNSELECTED = "SESSION_UNSELECTED";
    public static final String SESSION_DELETED = "SESSION_DELETED";
    public static final String SESSION_CREATED = "SESSION_CREATED";

    // Session-Player-Rule relationship commands
    public static final String SESSION_REQUEST = "SESSION_REQUEST";
    public static final String SESSION_LOADED = "SESSION_LOADED";
    public static final String SESSION_CHANGED = "SESSION_CHANGED";

    public static final String RULE_ADDED_TO_PLAYER = "RULE_ADDED_TO_PLAYER";
    public static final String RULE_REMOVED_FROM_PLAYER = "RULE_REMOVED_FROM_PLAYER";

    // Piano key commands
    public static final String KEY_PRESSED = "KEY_PRESSED";
    public static final String KEY_HELD = "KEY_HELD";
    public static final String KEY_RELEASED = "KEY_RELEASED";

    // Logging commands
    public static final String LOG_DEBUG = "LOG_DEBUG";
    public static final String LOG_INFO = "LOG_INFO";
    public static final String LOG_WARN = "LOG_WARN";
    public static final String LOG_ERROR = "LOG_ERROR";

    // Database commands
    public static final String CLEAR_DATABASE = "CLEAR_DATABASE";
    public static final String CLEAR_INVALID_SESSIONS = "CLEAR_INVALID_SESSIONS";
    public static final String DATABASE_RESET = "DATABASE_RESET";
    public static final String LOAD_CONFIG = "LOAD_INSTRUMENTS_FROM_FILE";

    // Player editor commands
    public static final String SHOW_PLAYER_EDITOR = "SHOW_PLAYER_EDITOR";
    public static final String SHOW_PLAYER_EDITOR_OK = "SHOW_PLAYER_EDITOR_OK";
    public static final String SHOW_PLAYER_EDITOR_CANCEL = "SHOW_PLAYER_EDITOR_CANCEL";

    // New commands for each dial
    public static final String NEW_VALUE_LEVEL = "NEW_VALUE_LEVEL";
    public static final String NEW_VALUE_NOTE = "NEW_VALUE_NOTE";
    public static final String NEW_VALUE_SWING = "NEW_VALUE_SWING";
    public static final String NEW_VALUE_PROBABILITY = "NEW_VALUE_PROBABILITY";
    public static final String NEW_VALUE_VELOCITY_MIN = "NEW_VALUE_VELOCITY_MIN";
    public static final String NEW_VALUE_VELOCITY_MAX = "NEW_VALUE_VELOCITY_MAX";
    public static final String NEW_VALUE_RANDOM = "NEW_VALUE_RANDOM";
    public static final String NEW_VALUE_PAN = "NEW_VALUE_PAN";
    public static final String NEW_VALUE_SPARSE = "NEW_VALUE_SPARSE";
    public static final String NEW_VALUE_RATCHET_COUNT = "NEW_VALUE_RATCHET_COUNT";
    public static final String NEW_VALUE_RATCHET_INTERVAL = "NEW_VALUE_RATCHET_INTERVAL";

    public static final String TRANSPOSE_UP = "TRANSPOSE_UP";
    public static final String TRANSPOSE_DOWN = "TRANSPOSE_DOWN";

    public static final String RULE_DELETED = "RULE_DELETED";

    public static final String MINI_NOTE_SELECTED = "MINI_NOTE_SELECTED";

    public static final String ROOT_NOTE_SELECTED = "ROOT_NOTE_SELECTED";

    public static final String SCALE_SELECTED = "SCALE_SELECTED";
    public static final String FIRST_SCALE_SELECTED = "FIRST_SCALE_SELECTED";
    public static final String LAST_SCALE_SELECTED = "LAST_SCALE_SELECTED";
    public static final String NEXT_SCALE_SELECTED = "NEXT__SCALE_SELECTED";
    public static final String PREV_SCALE_SELECTED = "PREV_SCALE_SELECTED";

    public static final String WINDOW_CLOSING = "WINDOW_CLOSING";
    public static final String WINDOW_RESIZED = "WINDOW_RESIZED";

    public static final String INSTRUMENT_UPDATED = "INSTRUMENT_UPDATED";
    public static final String INSTRUMENTS_REFRESHED = "INSTRUMENTS_REFRESHED";

    public static final String TRANSPORT_STATE = "TRANSPORT_STATE";

    public static final String SONG_SELECTED = "SONG_SELECTED";
    public static final String SONG_UPDATED = "SONG_UPDATED";
    public static final String SONG_CHANGED = "SONG_CHANGED";
    public static final String SONG_DELETED = "SONG_DELETED";

    public static final String PATTERN_ADDED = "PATTERN_ADDED";
    public static final String PATTERN_REMOVED = "PATTERN_REMOVED";
    public static final String PATTERN_UPDATED = "PATTERN_UPDATED";
    public static final String PATTERN_PARAMS_CHANGED = "PATTERN_PARAMS_CHANGED";
    public static final String PATTERN_SAVED = "PATTERN_SAVED";
    public static final String PATTERN_LOADED = "PATTERN_LOADED";
    public static final String SAVE_PATTERN = "SAVE_PATTERN";
    public static final String LOAD_PATTERN = "LOAD_PATTERN";

    public static final String STEP_UPDATED = "STEP_UPDATED";

    public static final String INSTRUMENT_ADDED = "INSTRUMENT_ADDED";
    public static final String INSTRUMENT_REMOVED = "INSTRUMENT_REMOVED";

    public static final String USER_CONFIG_LOADED = "USER_CONFIG_LOADED";

    // System Commands
    public static final String SYSTEM_READY = "SYSTEM_READY";
    public static final String PLAYER_ROW_REFRESH = "PLAYER_ROW_REFRESH";

    public static final String UPDATE_TEMPO = "UPDATE_TEMPO";
    public static final String UPDATE_TIME_SIGNATURE = "UPDATE_TIME_SIGNATURE";
    public static final String METRONOME_STOP = "METRONOME_STOP";
    public static final String METRONOME_STARTED = "METRONOME_STARTED";
    public static final String METRONOME_STOPPED = "METRONOME_STOPPED";
    public static final String METRONOME_START = "METRONOME_START";
    public static final String PRESET_DOWN = "PRESET_DOWN";
    public static final String PRESET_UP = "PRESET_UP";
    public static final String PRESET_SELECTED = "PRESET_SELECTED";
    public static final String PRESET_CHANGED = "PRESET_CHANGED";
    public static final String TRANSPORT_RECORD_START = "TRANSPORT_RECORD_START";
    public static final String TRANSPORT_RECORD_STOP = "TRANSPORT_RECORD_STOP";
    public static final String SAVE_SESSION = "SAVE_SESSION";
    public static final String PLAYER_TABLE_REFRESH_REQUEST = "PLAYER_TABLE_REFRESH_REQUEST";
    public static final String RECORDING_STOPPED = "RECORDING_STOPPED";
    public static final String RECORDING_STARTED = "RECORDING_STARTED";
    public static final String SESSION_STARTING = "SESSION_STARTING";
    public static final String SESSION_STOPPED = "SESSION_STOPPED";
    public static final String SESSION_SAVED = "SESSION_SAVED";
    public static final String STATUS_UPDATE = "STATUS_UPDATE";
    public static final String CLEAR_STATUS = "CLEAR_STATUS";
    public static final String PLAY_TEST_NOTE = "PLAY_TEST_NOTE";
    public static final String SOUNDBANK_LOADED = "SOUNDBANK_LOADED";

    public static final String SOUNDBANK_UNLOADED = "SOUNDBANK_UNLOADED";
    public static final String SOUNDBANK_SELECTED = "SOUNDBANK_SELECTED";
    public static final String SOUNDBANK_UNSELECTED = "SOUNDBANK_UNSELECTED";
    public static final String SOUNDBANK_ADDED = "SOUNDBANK_ADDED";
    public static final String SOUNDBANK_UPDATED = "SOUNDBANK_UPDATED";
    public static final String SOUNDBANK_DELETED = "SOUNDBANK_DELETED";
    public static final String SOUNDBANK_SELECTED_CHANGED = "SOUNDBANK_SELECTED_CHANGED";
    public static final String SOUNDBANK_UNSELECTED_CHANGED = "SOUNDBANK_UNSELECTED_CHANGED";
    public static final String SOUNDBANK_SELECTED_REQUEST = "SOUNDBANK_SELECTED_REQUEST";
    public static final String SOUNDBANK_UNSELECTED_REQUEST = "SOUNDBANK_UNSELECTED_REQUEST";

    public static final String PAD_TOGGLED = "PAD_TOGGLED";
    public static final String NEW_VALUE_OCTAVE = "NEW_VALUE_OCTAVE";
    public static final String SEQUENCER_STEP_UPDATE = "SEQUENCER_STEP_UPDATE";

    /**
     * Command for drum pad selection events
     */
    public static final String DRUM_PAD_SELECTED = "DRUM_PAD_SELECTED";
    public static final String DRUM_SEQUENCE_SAVED = "DRUM_SEQUENCE_SAVED";
    public static final String DRUM_SEQUENCE_UPDATED = "DRUM_SEQUENCE_UPDATED";
    public static final String DRUM_SEQUENCE_REMOVED = "DRUM_SEQUENCE_REMOVED";
    public static final String DRUM_SEQUENCE_ADDED = "DRUM_SEQUENCE_ADDED";
    public static final String DRUM_SEQUENCE_SELECTED = "DRUM_SEQUENCE_SELECTED";
    public static final String SAVE_DRUM_SEQUENCE = "SAVE_DRUM_SEQUENCE";
    public static final String LOAD_DRUM_SEQUENCE = "LOAD_DRUM_SEQUENCE";
    public static final String DRUM_SEQUENCE_PARAMS_CHANGED = "DRUM_SEQUENCE_PARAMS_CHANGED";
    public static final String DRUM_SEQUENCE_LOADED = "DRUM_SEQUENCE_LOADED";
    public static final String DRUM_SEQUENCES_ALL_DELETED = "DRUM_SEQUENCES_ALL_DELETED";

    public static final String MELODIC_SEQUENCE_SAVED = "MELODIC_SEQUENCE_SAVED";
    public static final String MELODIC_SEQUENCE_UPDATED = "MELODIC_SEQUENCE_UPDATED";
    public static final String MELODIC_SEQUENCE_LOADED = "MELODIC_SEQUENCE_LOADED";
    public static final String MELODIC_SEQUENCE_REMOVED = "MELODIC_SEQUENCE_REMOVED";
    public static final String MELODIC_SEQUENCE_ADDED = "MELODIC_SEQUENCE_ADDED";
    public static final String MELODIC_SEQUENCE_SELECTED = "MELODIC_SEQUENCE_SELECTED";

    public static final String MELODIC_SEQUENCER_ADDED = "MELODIC_SEQUENCER_ADDED";
    public static final String MELODIC_SEQUENCER_REMOVED = "MELODIC_SEQUENCER_REMOVED";

    // Navigation commands
    public static final String MELODIC_SEQUENCE_NEXT = "MELODIC_SEQUENCE_NEXT";
    public static final String MELODIC_SEQUENCE_PREV = "MELODIC_SEQUENCE_PREV";
    public static final String MELODIC_SEQUENCE_FIRST = "MELODIC_SEQUENCE_FIRST";
    public static final String MELODIC_SEQUENCE_LAST = "MELODIC_SEQUENCE_LAST";
    public static final String DRUM_SEQUENCE_CREATED = "DRUM_SEQUENCE_CREATED";
    public static final String TRANSPORT_RESET = "TRANSPORT_RESET";
    public static final String DRUM_SEQUENCE_FIRST = "DRUM_SEQUENCE_FIRST";
    public static final String DRUM_SEQUENCE_LAST = "DRUM_SEQUENCE_LAST";
    public static final String DRUM_NOTE_TRIGGERED = "DRUM_NOTE_TRIGGERED";
    public static final String MELODIC_NOTE_TRIGGERED = "MELODIC_NOTE_TRIGGERED";
    public static final String TOGGLE_TRANSPORT = "TOGGLE_TRANSPORT";
    public static final String LFO_VALUE_CHANGED = "LFO_VALUE_CHANGED";
    public static final String LFO_SELECTED = "LFO_SELECTED";
    public static final String DRUM_PATTERN_SWITCHED = "DRUM_PATTERN_SWITCHED";
    public static final String MELODIC_PATTERN_SWITCHED = "MELODIC_PATTERN_SWITCHED";
    public static final String GLOBAL_LOOPING_ENABLED = "GLOBAL_LOOPING_ENABLED";
    public static final String GLOBAL_LOOPING_DISABLED = "GLOBAL_LOOPING_DISABLED";

    // Add these new command constants
    public static final String SHOW_MAX_LENGTH_DIALOG = "SHOW_MAX_LENGTH_DIALOG";
    public static final String SHOW_EUCLIDEAN_DIALOG = "SHOW_EUCLIDEAN_DIALOG";
    public static final String SHOW_FILL_DIALOG = "SHOW_FILL_DIALOG";
    public static final String MAX_LENGTH_SELECTED = "MAX_LENGTH_SELECTED";
    public static final String EUCLIDEAN_PATTERN_SELECTED = "EUCLIDEAN_PATTERN_SELECTED";
    public static final String FILL_PATTERN_SELECTED = "FILL_PATTERN_SELECTED";

    public static final String TIMING_DIVISION_CHANGED = "TIMING_DIVISION_CHANGED";
    public static final String MELODIC_SEQUENCE_CREATED = "MELODIC_SEQUENCE_CREATED";
    public static final String DRUM_BUTTON_SELECTED = "DRUM_BUTTON_SELECTED";
    public static final String GLOBAL_SCALE_SELECTION_EVENT = "GLOBAL_SCALE_SELECTION_EVENT";
    public static final String DRUM_SEQUENCE_MAX_LENGTH_CHANGED = "DRUM_SEQUENCE_MAX_LENGTH_CHANGED";
    public static final String DRUM_SEQUENCE_GRID_RECREATE_REQUESTED = "DRUM_SEQUENCE_GRID_RECREATE_REQUESTED";
    public static final String USER_CONFIG_UPDATED = "USER_CONFIG_UPDATED";
    public static final String SESSION_TEMPO_CHANGED = "SESSION_TEMPO_CHANGED";
    public static final String THEME_CHANGED = "THEME_CHANGED";
    public static final String REQUEST_ONE_TIME_NOTIFICATION = "REQUEST_ONE_TIME_NOTIFICATION";
    public static final String SEQUENCER_STATE_CHANGED = "SEQUENCER_STATE_CHANGED";
    public static final String INSTRUMENT_CHANGED = "INSTRUMENT_CHANGED";
    public static final String DRUM_STEP_UPDATED = "DRUM_STEP_UPDATED";


    // Player preset commands
    public static final String PLAYER_PRESET_CHANGE_REQUEST = "PLAYER_PRESET_CHANGE_REQUEST";
    public static final String PLAYER_PRESET_CHANGED = "PLAYER_PRESET_CHANGED";

    // Player instrument commands
    public static final String PLAYER_CHANNEL_CHANGE_REQUEST = "PLAYER_CHANNEL_CHANGE_REQUEST";
    public static final String PLAYER_INSTRUMENT_CHANGE_REQUEST = "PLAYER_INSTRUMENT_CHANGE_REQUEST";
    public static final String PLAYER_INSTRUMENT_CHANGED = "PLAYER_INSTRUMENT_CHANGED";
    public static final String TRANSPORT_STATE_REQUEST = "TRANSPORT_STATE_REQUEST";
    public static final String TEMPO_CHANGE = "TEMPO_CHANGE";
    public static final String TIME_SIGNATURE_CHANGE = "TIME_SIGNATURE_CHANGE";
    public static final String ACTIVE_PLAYER_REQUEST = "ACTIVE_PLAYER_REQUEST";

    // Add this if it doesn't exist
    public static final String MELODIC_PATTERN_UPDATED = "MELODIC_PATTERN_UPDATED";
    public static final String DRUM_PLAYER_INSTRUMENT_CHANGED = "DRUM_PLAYER_INSTRUMENT_CHANGED";
    public static final String CREATE_INSTRUMENT_FOR_PLAYER_REQUEST = "CREATE_INSTRUMENT_FOR_PLAYER_REQUEST";

    // Add these constants to the Commands class
    public static final String LOAD_SOUNDBANK = "LOAD_SOUNDBANK";
    public static final String REFRESH_SOUNDBANKS = "REFRESH_SOUNDBANKS";
    public static final String SOUNDBANKS_REFRESHED = "SOUNDBANKS_REFRESHED";

    public static final String CHANNEL_ASSIGNMENT_CHANGED = "CHANNEL_ASSIGNMENT_CHANGED";

    public static final String DELETE_UNUSED_INSTRUMENTS = "DELETE_UNUSED_INSTRUMENTS";
    public static final String UPDATE_STATUS = "UPDATE_STATUS";
    public static final String REPAIR_MIDI_CONNECTIONS = "REPAIR_MIDI_CONNECTIONS";
    public static final String DRUM_SEQUENCE_DELETED = "DRUM_SEQUENCE_DELETED";
    public static final String PLAYER_LEVEL_CHANGED = "PLAYER_LEVEL_CHANGED";

    // Add these constants
    public static final String DRUM_PRESET_SELECTION_REQUEST = "drum.preset.selection.request";
    public static final String DRUM_INSTRUMENTS_UPDATED = "drum.instruments.updated";

    // Add a new command:
    public static final String REFRESH_ALL_INSTRUMENTS = "refresh.all.instruments";
    public static final String REFRESH_PLAYER_INSTRUMENT = "refresh.player.instrument";

    // Add these new command constants to the Commands class
    public static final String PLAYER_SELECTION_EVENT = "player.selection.event";
    public static final String PLAYER_UPDATE_EVENT = "player.update.event";
    public static final String PLAYER_INSTRUMENT_CHANGE_EVENT = "player.instrument.change.event";
    public static final String PLAYER_PRESET_CHANGE_EVENT = "player.preset.change.event";
    public static final String PLAYER_REFRESH_EVENT = "player.refresh.event";

    // Add to Commands.java:
    public static final String PLAYER_RULE_UPDATE_EVENT = "PLAYER_RULE_UPDATE_EVENT";

    // In Commands.java
    public static final String PAD_HIT = "PAD_HIT";

    // Add these new command constants for default player/instrument updates

    public static final String DEBUG_USER_CONFIG_SAVE = "DEBUG_USER_CONFIG_SAVE";
    public static final String SOUNDBANK_CHANGED = "SOUNDBANK_CHANGED";
    public static final String SAMPLE_LOOP_POINTS_CHANGED = "SAMPLE_LOOP_POINTS_CHANGED";
    public static final String SAMPLE_SELECTION_CHANGED = "SAMPLE_SELECTION_CHANGED";
    public static final String DRUM_STEP_PARAMETERS_CHANGED = "DRUM_STEP_PARAMETERS_CHANGED";
    public static final String DRUM_STEP_EFFECTS_CHANGED = "DRUM_STEP_EFFECTS_CHANGED";

    public static final String ENSURE_MIDI_CONNECTIONS = "ENSURE_MIDI_CONNECTIONS";
    public static final String REFRESH_MIDI_DEVICES = "REFRESH_MIDI_DEVICES";
    public static final String DRUM_STEP_SELECTED = "DRUM_STEP_SELECTED";
    public static final String HIGHLIGHT_STEP = "HIGHLIGHT_STEP";
    public static final String MELODIC_SEQUENCE_DELETED = "MELODIC_SEQUENCE_DELETED";
    public static final String HIGHLIGHT_SCALE_NOTE = "HIGHLIGHT_SCALE_NOTE";
    public static final String MODULATION_VALUE_CHANGED = "MODULATION_VALUE_CHANGED";

    // Add this constant for sequencer-specific root note changes
    public static final String SEQUENCER_ROOT_NOTE_SELECTED = "SEQUENCER_ROOT_NOTE_SELECTED";
    public static final String TOGGLE_LOOPING = "TOGGLE_LOOPING";
    public static final String SET_MAX_LENGTH = "SET_MAX_LENGTH";
    public static final String SAVE_ALL_MELODIC = "SAVE_ALL_MELODIC";

    public static final String SEQUENCER_TILT_FOLLOW_EVENT = "SEQUENCER_TILT_FOLLOW_EVENT";
    public static final String SEQUENCER_NOTE_FOLLOW_EVENT = "SEQUENCER_NOTE_FOLLOW_EVENT";
    public static final String DRUM_TRACK_MUTE_CHANGED = "DRUM_TRACK_MUTE_CHANGED";
    public static final String DRUM_TRACK_MUTE_VALUES_CHANGED = "DRUM_TRACK_MUTE_VALUES_CHANGED";
    public static final String SEQUENCER_SYNC_MESSAGE = "SEQUENCER_SYNC_MESSAGE";
    public static final String LOOPING_TOGGLE_EVENT = "LOOPING_TOGGLE_EVENT";
}