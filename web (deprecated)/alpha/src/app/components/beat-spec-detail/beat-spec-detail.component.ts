import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Instrument } from 'src/app/models/instrument';
import { Pattern } from 'src/app/models/pattern';
import { PatternUpdateType } from 'src/app/models/pattern-update-type';
import { MidiService } from 'src/app/services/midi.service';

@Component({
  selector: 'app-beat-spec-detail',
  templateUrl: './beat-spec-detail.component.html',
  styleUrls: ['./beat-spec-detail.component.css'],
})
export class BeatSpecDetailComponent {
  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Input()
  songId!: number;

  @Input()
  pattern!: Pattern;

  @Input()
  instruments!: Instrument[];

  identifier = 'beat-spec-detail';

  channels = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15];

  columns: string[] = [
    '',
    '',
    '',
    '',
    'Set',
    'Pattern',
    'Device',
    'Chan',
    'Preset',
    'Root',
    'Transp',
    'Length',
    'Start',
    'End',
    'Gate',
    'Speed',
    'Repeat',
    'Swing',
    'Random',
  ];

  constructor(private midiService: MidiService) {}

  onInstrumentSelected(instrument: Instrument) {
    this.instrumentSelectEvent.emit(instrument);
  }

  onChannelChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.CHANNEL,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onPresetChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.PRESET,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onActiveChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.ACTIVE,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onRootNoteChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.ROOT_NOTE,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onDeviceChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.DEVICE,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onDirectionChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.DIRECTION,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onGateChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.GATE,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onFirstStepChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.FIRST_STEP,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onLastStepChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.LAST_STEP,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onLengthChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.LENGTH,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onQuantizeChange() {
    this.midiService
      .updatePattern(this.pattern.id, PatternUpdateType.QUANTIZE, 0)
      .subscribe((data) => (this.pattern = data));
  }

  onLinkChange() {
    // this.midiService.updatePattern(this.pattern.id, PatternUpdateType.QUANTIZE, 0).subscribe(data => this.pattern = data)
  }

  onLoopChange() {
    this.midiService
      .updatePattern(this.pattern.id, PatternUpdateType.LOOP, 0)
      .subscribe((data) => (this.pattern = data));
  }

  onMuteChange() {
    this.midiService
      .updatePattern(this.pattern.id, PatternUpdateType.MUTE, 0)
      .subscribe((data) => (this.pattern = data));
  }

  onRandomChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.RANDOM,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onRepeatsChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.REPEATS,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onSpeedChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.SPEED,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onSwingChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.SWING,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  onTransposeChange(event: { target: any }) {
    this.midiService
      .updatePattern(
        this.pattern.id,
        PatternUpdateType.TRANSPOSE,
        event.target.value
      )
      .subscribe((data) => (this.pattern = data));
  }

  getStepOptions() {
    let result: number[] = [];
    this.pattern.steps.forEach((step) => result.push(step.position));
    return result;
  }

  getTransposeOptions() {
    let result: number[] = [-4, -3, -2, -1, 0, 1, 2, 3, 4];
    return result;
  }
}
