export interface MessageListener {
  notify(_messageType: number, _message: string, messageValue: number): any
}
