import { Caption } from "./caption"

export interface ControlCode {
  id: number
  name: string
  code: number
  lowerBound: number
  upperBound: number
  captions: Caption[]
}
