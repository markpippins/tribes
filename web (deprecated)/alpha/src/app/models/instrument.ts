import {ControlCode} from "./control-code"

export interface Instrument {
  id: number
  available: boolean
  name: string
  deviceName: string
  channel: number
  channels: number[]; // for multi-channel instruments
  lowestNote: number
  highestNote: number
  highestPreset: number
  preferredPreset: number
  assignments: Map<number, string>
  boundaries: Map<number, number[]>
  hasAssignments: boolean
  pads: number
  controlCodes: ControlCode[]
}
