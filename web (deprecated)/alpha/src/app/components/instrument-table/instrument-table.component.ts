import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Device } from 'src/app/models/device';
import { Instrument } from 'src/app/models/instrument';

@Component({
  selector: 'app-instrument-table',
  templateUrl: './instrument-table.component.html',
  styleUrls: ['./instrument-table.component.css'],
})
export class InstrumentTableComponent {
  updateHighNote(_$event: Event, _t14: Instrument) {
    throw new Error('Method not implemented.');
  }
  updateLowNote(_$event: Event, _t14: Instrument) {
    throw new Error('Method not implemented.');
  }
  updateAvailable(event: Event, instrument: Instrument) {
    const checkbox = event.target as HTMLInputElement;
    instrument.available = checkbox.checked;
  }
  updateName(_$event: Event, _t14: Instrument) {
    throw new Error('Method not implemented.');
  }
  @Input()
  instruments!: Instrument[];

  @Input()
  devices!: Device[];

  selectedInstrument!: Instrument;

  channels = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15];

  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  getRowClass(instrument: Instrument): string {
    // if (_device.description.includes('External MIDI Port')) return 'port-table-row';

    // if (_device.description.includes('Internal software synthesizer')) return 'synth-table-row';

    // if (_device.description.includes('Software MIDI Synthesizer')) return 'synth-table-row';

    // if (_device.description.includes('Software sequencer')) return 'sequencer-table-row';

    // if (_device.description.includes('MAPPER')) return 'midi-mapper-table-row ';

    // if (_device.description.includes('No details available')) return 'other-table-row';


    return instrument == this.selectedInstrument ? 'active-table-row' : 'table-row';
  }

  onRowClick(instrument: Instrument) {
    this.selectedInstrument = instrument;
    this.instrumentSelectEvent.emit(instrument);
  }
}
