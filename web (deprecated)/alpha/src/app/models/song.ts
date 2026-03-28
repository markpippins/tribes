import { Pattern } from "./pattern"

export interface Song {
  id: number
  name: string
  patterns: Pattern[]
}
