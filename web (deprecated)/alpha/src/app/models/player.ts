import { Rule } from './rule';

export interface Player {
  id: number
  playerClass: string
  tickerId: number
  instrumentId: number
  channel: number
  preset: number
  rules: Rule[]
  allowedControlMessages: number[]
  parts: number[]
  note: number
  minVelocity: number
  maxVelocity: number
  probability: number
  muted: boolean
  name: string
  swing: number
  level: number
  active: boolean
  skips: number
  subDivisions: number
  beatFraction: number
  randomDegree: number
  ratchetCount: number
  ratchetInterval: number
  fadeIn: number
  fadeOut: number
  solo: boolean
}
