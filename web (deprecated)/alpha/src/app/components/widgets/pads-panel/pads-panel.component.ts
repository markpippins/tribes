import {Component, EventEmitter, OnInit} from '@angular/core';
import {Instrument} from "../../../models/instrument";
import {MidiService} from "../../../services/midi.service";

@Component({
  selector: 'app-pads-panel',
  templateUrl: './pads-panel.component.html',
  styleUrls: ['./pads-panel.component.css']
})
export class PadsPanelComponent implements OnInit {

  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.onSelect(this.channel);
  }

  firstNote = 36;
  channel: number = 10;
  instrument!: Instrument;
  channelSelectEvent = new EventEmitter<number>();
  rows = [[
    '13', '14', '15', '16',
    '9', '10', '11', '12',
    '5', '6', '7', '8',
    '1', '2', '3', '4'
  ]];

  getNote(row: string[], column: number) {


    return this.rows.indexOf(row) * 100 + column;
  }

  onSelect(selectedChannel: number) {
    this.midiService
      .instrumentInfoByChannel(this.instrument.deviceName, selectedChannel - 1)
      .subscribe(async (data) => {
        this.instrument = data;
        this.channel = selectedChannel;
        this.channelSelectEvent.emit(selectedChannel);
      });
  }
}
