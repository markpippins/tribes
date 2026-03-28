import {Player} from "./player"

export interface Ticker {
  id: number
  tick: number
  tickCount: number
  ticksPerBeat: number
  done: boolean
  bar: number
  bars: number
  barCount: number
  beat: number
  beats: number
  beatCount: number
  part: number
  parts: number
  partCount: number
  beatsPerBar: number
  beatDivider: number
  tempoInBPM: number
  partLength: number
  maxTracks: number
  songLength: number
  swing: number
  playing: boolean
  stopped: boolean
  activePlayerIds: number[]
  noteOffset: number
  players: Player[]
}
