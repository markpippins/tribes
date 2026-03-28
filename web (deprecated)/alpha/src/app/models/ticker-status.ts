import { PatternStatus } from "./pattern-status"

export interface TickerStatus {
  id: number
  bars: number
  beatsPerBar: number
  beatDivider: number
  partLength: number
  maxTracks: number
  songLength: number
  swing: number
  ticksPerBeat: number
  tempoInBPM: number
  loopCount: number
  parts: number
  noteOffset: number
  playing: boolean
  tick: number
  beat: number
  bar: number
  part: number
  tickCount: number
  beatCount: number
  barCount: number
  partCount: number
  patternStatuses: PatternStatus[]
  playerCount: number
  hasSolos: boolean
}
