import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import { Subscription } from 'rxjs';
import { Constants } from 'src/app/models/constants';
import { TickListener } from 'src/app/models/tick-listener';
import { TickerStatus } from 'src/app/models/ticker-status';
import { TickerUpdateType } from 'src/app/models/ticker-update-type';
import { TickerService } from 'src/app/services/ticker.service';
import { UiService } from 'src/app/services/ui.service';
import { MidiService } from '../../services/midi.service';
// import { SongStatus } from 'src/app/models/song-status';

@Component({
  selector: 'app-status-panel',
  templateUrl: './status-panel.component.html',
  styleUrls: ['./status-panel.component.css'],
})
export class StatusPanelComponent implements TickListener, OnDestroy, OnInit {
  tickerSubscription!: Subscription;
  // songSubscription!: Subscription;
  ppqSelectionIndex!: number;
  ppqs = [
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 18, 21, 23, 24, 27,
    29, 32, 33, 36, 37, 40, 42, 44, 46, 47, 48, 62, 64, 72, 74, 77, 84, 87, 88,
    89, 96,
  ];

  statusColumns = [
    'Ticker',
    'Tick',
    'Beat',
    'Bar',
    '',
    'PPQ',
    'BPM',
    'Beats / Bar',
    'Part Length',
    'Max',
  ];

  @Input()
  tickerStatus!: TickerStatus;

  @Output()
  ppqChangeEvent = new EventEmitter<number>();

  @Output()
  tempoChangeEvent = new EventEmitter<number>();

  colors = ['red', 'orange', 'yellow', 'green', 'blue', 'indigo', 'violet'];
  index = 0;
  waiting = false;
  nextCalled = false;

  lastBeat = 0;
  lastBar = 0;
  lastPart = 0;

  constructor(
    private midiService: MidiService,
    private uiService: UiService,
    private tickerService: TickerService
  ) {
  }

  ngOnDestroy(): void {
    this.tickerService.removeListener(this)
    this.tickerSubscription && this.tickerSubscription.unsubscribe();
  }

  ngOnInit(): void {
    this.tickerService.addListener(this)
  }

  update(tickerStatus: TickerStatus): void {
    this.cycleColors();
    this.tickerStatus = tickerStatus;
    if (this.tickerStatus.tickCount % this.tickerStatus.ticksPerBeat == 0)
      this.uiService.notifyAll(Constants.BEAT_DIV, '', this.tickerStatus.tickCount);
    this.updateDisplay();
  }


  cycleColors() {
    let colorContainer = document.getElementById('dashboard') as HTMLElement;
    if (colorContainer) {
      colorContainer.style.backgroundColor = this.colors[this.index];
      this.index = (this.index + 1) % this.colors.length;
    }
  }

  getBeats() {
    const beats = [];
    for (let i = this.tickerStatus.beatsPerBar; i >= 1; i--) beats.push(i);
    return beats.reverse();
  }

  updateDisplay(): void {
    if (this.tickerStatus != undefined) {
      this.tickerStatus.patternStatuses.forEach((patternStatus) =>
        this.uiService.notifyAll(
          Constants.NOTIFY_SONG_STATUS,
          '',
          patternStatus
        )
      );

      if (this.tickerStatus.beat != this.lastBeat) {
        this.lastBeat = this.tickerStatus.beat;
        this.uiService.notifyAll(
          Constants.BEAT_DIV,
          '',
          this.tickerStatus.beat
        );

        if (this.tickerStatus.bar != this.lastBar) {
          this.lastBar = this.tickerStatus.bar;
          this.uiService.notifyAll(
            Constants.BAR_DIV,
            '',
            this.tickerStatus.bar
          );
        }

        if (this.tickerStatus.part != this.lastPart) {
          this.lastPart = this.tickerStatus.part;
          this.uiService.notifyAll(
            Constants.PART_DIV,
            '',
            this.tickerStatus.part
          );
        }
      }
    }
  }

  onTempoChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.BPM,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Tempo changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onBeatsPerBarChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.BEATS_PER_BAR,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'BPM changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onBarsChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.BARS,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Bars changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPartsChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.PARTS,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'Parts changed', 0)
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPartLengthChange(event: { target: any }) {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.PART_LENGTH,
        event.target.value
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(
          Constants.TICKER_UPDATED,
          'Part Length changed',
          0
        )
      );
    this.tempoChangeEvent.emit(event.target.value);
  }

  onPPQSelectionChange() {
    this.midiService
      .updateTicker(
        this.tickerStatus.id,
        TickerUpdateType.PPQ,
        this.ppqs[this.ppqSelectionIndex]
      )
      .subscribe((_data) =>
        this.uiService.notifyAll(Constants.TICKER_UPDATED, 'PPQ changed', 0)
      );
    this.ppqChangeEvent.emit(this.ppqs[this.ppqSelectionIndex]);
  }

  setIndexForPPQ() {
    this.ppqSelectionIndex = this.ppqs.indexOf(this.tickerStatus.ticksPerBeat);
  }

  getTickerPosition() {
    return Math.round(this.tickerStatus.beat);
  }
}
