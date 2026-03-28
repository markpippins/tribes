export const MidiMessage = {
  // System common messages

  /**
   * Status byte for MIDI Time Code Quarter Frame message (0xF1, or 241).
   *
   */
  MIDI_TIME_CODE: 0xf1, // 241

  /**
   * Status byte for Song Position Pointer message (0xF2, or 242).
   *
   */
  SONG_POSITION_POINTER: 0xf2, // 242

  /**
   * Status byte for MIDI Song Select message (0xF3, or 243).
   *
   */
  SONG_SELECT: 0xf3, // 243

  /**
   * Status byte for Tune Request message (0xF6, or 246).
   *
   */
  TUNE_REQUEST: 0xf6, // 246

  /**
   * Status byte for End of System Exclusive message (0xF7, or 247).
   *
   */
  END_OF_EXCLUSIVE: 0xf7, // 247

  // System real-time messages

  /**
   * Status byte for Timing Clock message (0xF8, or 248).
   *
   */
  TIMING_CLOCK: 0xf8, // 248

  /**
   * Status byte for Start message (0xFA, or 250).
   *
   */
  START: 0xfa, // 250

  /**
   * Status byte for Continue message (0xFB, or 251).
   *
   */
  CONTINUE: 0xfb, // 251

  /**
   * Status byte for Stop message (0xFC, or 252).
   *
   */
  STOP: 0xfc, //252

  /**
   * Status byte for Active Sensing message (0xFE, or 254).
   *
   */
  ACTIVE_SENSING: 0xfe, // 254

  /**
   * Status byte for System Reset message (0xFF, or 255).
   *
   */
  SYSTEM_RESET: 0xff, // 255

  // Channel voice message upper nibble defines

  /**
   * Command value for Note Off message (0x80, or 128).
   */
  NOTE_OFF: 0x80, // 128

  /**
   * Command value for Note On message (0x90, or 144).
   */
  NOTE_ON: 0x90, // 144

  /**
   * Command value for Polyphonic Key Pressure (Aftertouch) message (0xA0, or
   * 160).
   */
  POLY_PRESSURE: 0xa0, // 160

  /**
   * Command value for Control Change message (0xB0, or 176).
   */
  CONTROL_CHANGE: 0xb0, // 176

  /**
   * Command value for Program Change message (0xC0, or 192).
   */
  PROGRAM_CHANGE: 0xc0, // 192

  /**
   * Command value for Channel Pressure (Aftertouch) message (0xD0, or 208).
   */
  CHANNEL_PRESSURE: 0xd0, // 208

  /**
   * Command value for Pitch Bend message (0xE0, or 224).
   */
  PITCH_BEND: 0xe0, // 224

  /**
   * Constructs a new {@code ShortMessage}. The contents of the new message
   * are guaranteed to specify a valid MIDI message. Subsequently, you may set
   * the contents of the message using one of the {@code setMessage} methods.
   *
   */
  // public ShortMessage() {
  //   this(new byte[3]),
  //   // Default message data: NOTE_ON on Channel 0 with max volume
  //   data[0] : (byte) (NOTE_ON & 0xFF),
  //   data[1] : (byte) 64,
  //   data[2] : (byte) 126,
  //   length : 3,
  // }
};
