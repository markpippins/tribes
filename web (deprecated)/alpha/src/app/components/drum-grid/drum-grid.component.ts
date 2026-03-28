import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Instrument} from "../../models/instrument";
import {MidiService} from "../../services/midi.service";

@Component({
  selector: 'app-drum-grid',
  templateUrl: './drum-grid.component.html',
  styleUrls: ['./drum-grid.component.css']
})
export class DrumGridComponent implements OnInit {
  @Input()
  firstNote = 36;

  @Input()
  channel: number = 10;

  @Output()
  instrument!: Instrument;

  @Output()
  channelSelectEvent = new EventEmitter<number>();

  @Input()
  rows = [
    ['Ride', 'Clap', 'Perc', 'Bass'],
    ['Tom', 'Clap', 'Wood', 'P1'],
    ['Ride', 'fx', 'Perc', 'Bass'],
    ['Kick', 'Snare', 'Closed Hat', 'Open Hat'],
  ];

  constructor(private midiService: MidiService) {}

  getNote(row: string[], column: number) {
    return (
      this.firstNote +
      column +
      Math.abs(this.rows.length - 1 - this.rows.indexOf(row)) * this.rows.length
    );
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

  ngOnInit(): void {
    this.onSelect(this.channel);
  }
}
