import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Instrument } from "../../../models/instrument";
import { MidiService } from "../../../services/midi.service";

@Component({
  selector: 'app-channel-selector',
  templateUrl: './channel-selector.component.html',
  styleUrls: ['./channel-selector.component.css']
})
export class ChannelSelectorComponent implements OnInit {
  @Output()
  channelSelectEvent = new EventEmitter<number>();

  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Output()
  midiChannels = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16];

  @Input()
  channel: number = 10;
  instrument?: Instrument | undefined;
  constructor(private midiService: MidiService) {
  }

  ngOnInit(): void {
    this.onChannelChanged(1);
  }

  selectionChange(event: { target: any; }) {
    this.onChannelChanged(event.target.value);
  }

  onChannelChanged(newChannel: number) {
    this.channel = newChannel;
    if (this.instrument?.deviceName) {
      this.midiService
        .instrumentInfoByChannel(this.instrument.deviceName, this.channel - 1)
        .subscribe(async (data) => {
          this.instrument = data;
          this.channelSelectEvent.emit(this.channel);
          this.instrumentSelectEvent.emit(this.instrument);
        });
    }
  }
}
