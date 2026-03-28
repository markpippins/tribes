import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ControlCode } from 'src/app/models/control-code';
import { Instrument } from 'src/app/models/instrument';

@Component({
  selector: 'app-control-codes-table',
  templateUrl: './control-codes-table.component.html',
  styleUrls: ['./control-codes-table.component.css']
})
export class ControlCodesTableComponent {
  @Input()
  instrument!: Instrument
  selectedControlCode!: ControlCode

  @Output()
  controlCodeEvent = new EventEmitter<ControlCode>();

  getRowClass(_cc: ControlCode): string {
    // if (_device.description.includes('External MIDI Port')) return 'port-table-row';

    // if (_device.description.includes('Internal software synthesizer')) return 'synth-table-row';

    // if (_device.description.includes('Software MIDI Synthesizer')) return 'synth-table-row';

    // if (_device.description.includes('Software sequencer')) return 'sequencer-table-row';

    // if (_device.description.includes('MAPPER')) return 'midi-mapper-table-row ';

    // if (_device.description.includes('No details available')) return 'other-table-row';

    // instrument == this.selectedInstrument ? 'active-table-row' :
    return  'table-row';
  }

  onRowClick(controlCode: ControlCode) {
    this.selectedControlCode = controlCode;
    this.controlCodeEvent.emit(controlCode);
  }
}
