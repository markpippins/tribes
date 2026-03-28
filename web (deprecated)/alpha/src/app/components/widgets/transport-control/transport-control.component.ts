import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core'
import { Constants } from 'src/app/models/constants'
import { MessageListener } from 'src/app/models/message-listener'
import { UiService } from 'src/app/services/ui.service'

@Component({
  selector: 'app-transport-control',
  templateUrl: './transport-control.component.html',
  styleUrls: ['./transport-control.component.css']
})
export class TransportControlComponent implements OnDestroy, OnInit, MessageListener {

  @Output()
  clickEvent = new EventEmitter<string>()

  connected = false;

  constructor(private uiService: UiService) {
  }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }

  ngOnInit(): void {
    this.uiService.addListener(this)
  }

  notify(_messageType: number, _message: string) {
    // console.log("NOTIFIED")
    if (_messageType == Constants.CONNECTED)
      this.connected = true
    else if (_messageType == Constants.DISCONNECTED)
      this.connected = false
  }


  onClick(action: string) {
    this.clickEvent.emit(action)
  }

}
