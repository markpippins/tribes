export interface Device {
  name: string
  vendor: string
  description: string
  version: number
  maxReceivers: number
  maxTransmitters: number
  receiver: string
  receivers: string
  transmitter: string
  transmitters: string
  channels: number[]
}
