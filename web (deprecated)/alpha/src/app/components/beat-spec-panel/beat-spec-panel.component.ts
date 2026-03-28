import {
  AfterContentChecked,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  OnDestroy,
} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';
import { Step } from '../../models/step';
import { Instrument } from 'src/app/models/instrument';
import { Pattern } from 'src/app/models/pattern';
import { MessageListener } from 'src/app/models/message-listener';
interface PitchPair {
  midi: number;
  note: string;
}

@Component({
  selector: 'app-beat-spec-panel',
  templateUrl: './beat-spec-panel.component.html',
  styleUrls: ['./beat-spec-panel.component.css'],
})
export class BeatSpecPanelComponent
  implements MessageListener, OnInit, AfterContentChecked, OnDestroy {

  colors = [
    'violet',
    'lightsalmon',
    'lightseagreen',
    'deepskyblue',
    'fuchsia',
    'mediumspringgreen',
    'mediumpurple',
    'firebrick',
    'mediumorchid',
    'aqua',
    'olivedrab',
    'cornflowerblue',
    'lightcoral',
    'crimson',
    'goldenrod',
    'tomato',
    'blueviolet',
  ];

  @Output()
  paramBtnClickEvent = new EventEmitter<number>();

  @Output()
  changeEvent = new EventEmitter<Step>();

  @Input()
  pattern!: Pattern;

  @Input()
  swirling = true;

  @Input()
  step!: Step;

  @Input()
  instrument!: Instrument;

  @Output()
  active: boolean = false;

  pitchMap: PitchPair[] = [];

  lastBeat = 0;

  constructor(private uiService: UiService, private midiService: MidiService) { }

  ngAfterContentChecked(): void {
    // this.selected = this.step != undefined && this.step.active;
  }

  ngOnInit(): void {
    this.uiService.addListener(this);

    for (let i = 0; i < 126; i++)
      this.pitchMap.push({
        midi: i,
        note: this.uiService.getNoteForValue(i, Constants.SCALE_NOTES),
      });
  }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }


  notify(messageType: number, _message: string, _messageValue: any) {
    // console.log(this.getCaption() + "NOTIFIED")
    if (messageType == Constants.TICKER_STARTED) {
      console.log("TICKER_STARTED")
      this.lastBeat = 0;
      this.swirling = false;
    }

    if (messageType == Constants.TICKER_STOPPED) {
      console.log("TICKER_STOPPED")
      this.lastBeat = 0;
      this.swirling = true;
    }

    if (messageType == Constants.BEAT_DIV) {
      console.log("BEAT_DIV")
      if (this.lastBeat > this.pattern.lastStep)
        this.lastBeat = this.pattern.firstStep;
      else if (this.lastBeat == 0) this.lastBeat = 1;
      else
        this.active =
          // this.pattern.position == status.position &&
          this.lastBeat == this.step.position;
      this.lastBeat++;
    }
    // if (messageType == Constants.NOTIFY_SONG_STATUS) {
    //   let status: PatternStatus = messageValue;
    //   this.active =
    //     this.pattern.position == status.position &&
    //     status.activeStep == this.step.position;
    // }
  }

  onPadPressed(_index: number) {
    this.step.active = !this.step.active;
    // this.selected = !this.selected
  }

  onParamsBtnClick() {
    this.paramBtnClickEvent.emit(this.step.position);
    this.uiService.notifyAll(Constants.STEP_UPDATED, 'Step Updated', 0);
  }

  onLaneBtnClick() {
    this.step.active = !this.step.active;
    this.midiService
      .updateStep(this.step.id, this.step.position, Constants.STEP_ACTIVE, 1)
      .subscribe(async (data) => (this.step = data));

    let element = document.getElementById('beat-btn-' + this.step.position);
    if (this.uiService.hasClass(element, 'active')) {
      this.uiService.removeClass(element, 'active');
      this.uiService.addClass(element, 'inactive');
    } else if (this.uiService.hasClass(element, 'inactive'))
      this.uiService.removeClass(element, 'inactive');
    this.uiService.addClass(element, 'active');
  }

  onNoteChange(_event: { target: any }) {
    // alert(this.step.pitch)
    // if (this.step.pitch == event.target.value)
    this.midiService
      .updateStep(
        this.step.id,
        this.step.position,
        Constants.STEP_PITCH,
        this.step.pitch
      )
      .subscribe((data) => (this.step = data));
  }

  onVelocityChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
        step.position,
        Constants.STEP_VELOCITY,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onGateChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
        step.position,
        Constants.STEP_GATE,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onProbabilityChange(step: Step, event: { target: any }) {
    this.midiService
      .updateStep(
        step.id,
        step.position,
        Constants.STEP_PROBABILITY,
        event.target.value
      )
      .subscribe((data) => (this.step = data));
  }

  onChange() {
    this.changeEvent.emit(this.step);
  }

  getClass() {
    return this.active ? 'lane-808 ready' : 'lane-808';
  }

  getCaption(): string {
    return this.step.position.toString();
  }

  getMaxValue(_name: string): number {
    let result = 127;

    return result;
  }

  getMinValue(_name: string): number {
    return 0;
  }

  getRangeColor() {
    return 'slategrey';
  }

  getStrokeWidth() {
    return 10;
  }

  getStyleClass() {
    return 'knob';
  }

  getTextColor() {
    return 'white';
  }

  getValueColor(index: number) {
    return this.colors[index];
  }

  getValueTemplate(name: string): string {
    let result = name;

    switch (name) {
      case 'pitch': {
        result = this.uiService.getNoteForValue(
          this.step.pitch + this.pattern.transpose,
          Constants.SCALE_NOTES
        );
        break;
      }
    }

    return result;
  }
}
