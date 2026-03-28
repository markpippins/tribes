import {Component, Input} from '@angular/core'
import {Options} from "@angular-slider/ngx-slider"
import '@angular/animations'
import {MidiService} from "../../../services/midi.service"
import {MidiMessage} from "../../../models/midi-message"

@Component({
  selector: 'app-slider',
  templateUrl: './slider.component.html',
  styleUrls: ['./slider.component.css']
})
export class SliderComponent {

  @Input()
  instrumentId!: number

  @Input()
  channel!: number

  @Input()
  configMode!: boolean

  @Input()
  cc!: number

  @Input()
  label!: string
  value: number = 1
  options: Options = {
    floor: 1,
    ceil: 127,
    vertical: true,
    hideLimitLabels: false,
  }

  constructor(private midiService: MidiService) {
  }
  onChange() {
    this.midiService.sendMessage(this.instrumentId, this.channel, MidiMessage.CONTROL_CHANGE, this.cc, this.value)
  }
}
