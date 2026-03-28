import { Component, Input } from '@angular/core';
import { Instrument } from 'src/app/models/instrument';
import { MidiMessage } from 'src/app/models/midi-message';
import { MidiService } from 'src/app/services/midi.service';

@Component({
  selector: 'app-midi-knob',
  templateUrl: './midi-knob.component.html',
  styleUrls: ['./midi-knob.component.css'],
})
export class MidiKnobComponent {
  @Input()
  name!: string;

  colors = ['violet', 'lightsalmon', 'lightseagreen', 'deepskyblue', 'fuchsia', 'mediumspringgreen', 'mediumpurple', 'firebrick', 'mediumorchid', 'aqua', 'olivedrab', 'cornflowerblue', 'lightcoral', 'crimson', 'goldenrod', 'tomato', 'blueviolet']
  @Input()
  instrument!: Instrument

  @Input()
  cc!: number;

  @Input()
  panel: string = '';

  value: number = 50

  constructor(private midiService: MidiService) {
  }

  onChange() {
    this.midiService.sendMessage(this.instrument.id, this.instrument.channel, MidiMessage.CONTROL_CHANGE, this.cc, this.value)
  }

  getMaxValue() {
    return 127
  }

  getMinValue() {
    return 0
  }

  getRangeColor() {
    return 'slategrey';
  }

  getStrokeWidth() {
    return 20;
  }

  getStyleClass() {
    return 'knob';
  }

  getTextColor() {
    return 'white';
  }

  @Input()
  index!: number

  getValueColor() {
    return this.colors[this.index];
  }

  getValueTemplate() {
    return this.name.replace(this.panel, ' ');
  }
}
