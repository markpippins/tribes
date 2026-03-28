import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Comparison } from 'src/app/models/comparison';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { MessageListener } from 'src/app/models/message-listener';
import { Operator } from 'src/app/models/operator';
import { Player } from 'src/app/models/player';
import { Ticker } from 'src/app/models/ticker';
import { TickerStatus } from 'src/app/models/ticker-status';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-beat-navigator',
  templateUrl: './beat-navigator.component.html',
  styleUrls: ['./beat-navigator.component.css'],
})
export class BeatNavigatorComponent implements OnInit, MessageListener {

  @Input()
  ticker!: Ticker;

  divCount = 4;
  colCount = 23;

  ticksPosition = this.colCount;
  tickRange: number[] = [];
  tickOverflow: string[] = [];
  ticks: number[] = [];
  beats: number[] = [];
  bars: number[] = [];
  divs: number[] = [];
  parts: number[] = [];
  range: number[] = [];

  selectedTicks: boolean[] = [];
  selectedBeats: boolean[] = [];
  selectedBars: boolean[] = [];
  selectedDivs: boolean[] = [];
  selectedParts: boolean[] = [];
  selectedNote: number = 0;
  resolution: string[] = ['accent', 'tick', 'div', 'beat', 'bar', 'part'];

  comboId = 'beat-navigator';
  controlBtnClassName = 'mini-control-btn';
  newPartOnSum: boolean = false;

  instruments!: Instrument[];
  selectedInstrument!: Instrument;

  beatIndicators: boolean[] = [];
  barIndicators: boolean[] = [];
  partIndicators: boolean[] = [];

  constructor(
    private http: HttpClient,
    // private pl: PlatformLocation,
    private uiService: UiService,
    private midiService: MidiService
  ) {
    uiService.addListener(this);
  }

  ngOnInit(): void {
    this.midiService.allInstruments().subscribe((data) => {
      this.instruments = this.uiService.sortByName(data);
    });
    this.updateDisplay();
  }

  lastBeat = 0;

  ngAfterContentChecked(): void {
    if (
      this?.selectedInstrument == undefined &&
      this.instruments != undefined &&
      this.instruments.length > 0
    ) {
      this.selectedInstrument = this.instruments[0];
      this.onInstrumentSelected(this.instruments[0]);
    }
  }

  private _reqOptionsArgs = {
    headers: new HttpHeaders().set('Content-Type', 'application/json'),
  };

  getTickerStatus(): Observable<TickerStatus> {
    return this.http.get<TickerStatus>(
      'http://localhost:8080/api/foos',
      this._reqOptionsArgs
    );
  }

  testWebSocket() {
    this.getTickerStatus().subscribe((data) => console.log(data));
  }

  toggleNewPartOnSum(): void {
    this.newPartOnSum = !this.newPartOnSum;
  }

  generate() {
    if (this.selectedNote == undefined) return;

    this.midiService
      .addPlayerForNote(this.selectedInstrument.name, this.selectedNote)
      .subscribe((player) => {
        let partIndex = 0;
        this.selectedParts.forEach((part) => {
          if (part) {
            let partValue = partIndex + 1;
            // this.addRuleForPart(player, partValue);

            let barIndex = 0;
            this.selectedBars.forEach((bar) => {
              if (bar) {
                let barValue = barIndex + 1;
                this.addRuleForBar(player, barValue, partValue);

                let beatIndex = 0;
                this.selectedBeats.forEach((beat) => {
                  if (beat) {
                    let beatValue = beatIndex + 1;
                    this.addRuleForBeat(player, beatValue, partValue);

                    if (this.selectedTicks.includes(true)) {
                      let tickIndex = 0;
                      this.selectedTicks.forEach((tick) => {
                        if (tick) {
                          let tickValue = tickIndex + 1;
                          this.addRuleForTick(player, tickValue, partValue);
                          this.uiService.notifyAll(
                            Constants.PLAYER_UPDATED,
                            '',
                            0
                          );
                        }
                        tickIndex++;
                      });
                    } else {
                      this.midiService
                        .addPlayer(this.selectedInstrument.name)
                        .subscribe((player) => {
                          this.addRuleForTick(player, 1, 0);
                          this.addRuleForBeat(player, beatValue, 0);
                          this.addRuleForBar(player, barValue, 0);
                          // this.addRuleForPart(player, partValue);
                          this.uiService.notifyAll(
                            Constants.PLAYER_UPDATED,
                            '',
                            0
                          );
                          this.uiService.notifyAll(
                            Constants.PLAYER_UPDATED,
                            '',
                            0
                          );
                        });
                    }
                  }
                  beatIndex++;
                });
              }
              barIndex++;
            });
          }
        });

        partIndex++;
      });
  }

  addRuleForTick(player: Player, tick: number, part: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.TICK, Comparison.EQUALS, tick, part)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  addRuleForBeat(player: Player, beat: number, part: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.BEAT, Comparison.EQUALS, beat, part)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  addRuleForBar(player: Player, bar: number, part: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.BAR, Comparison.EQUALS, bar, part)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  addRuleForPart(player: Player, part: number) {
    this.midiService
      .addSpecifiedRule(player, Operator.PART, Comparison.EQUALS, part, 0)
      .subscribe((data) =>
        this.uiService.notifyAll(
          Constants.PLAYER_UPDATED,
          'Rule Added',
          data.id
        )
      );
  }

  getBeatPerBar(): number {
    return this.ticker == undefined ? 4 : this.ticker.beatsPerBar;
  }

  onInstrumentSelected(instrument: Instrument) {
    this.range = [];
    if (
      instrument.lowestNote > 0 &&
      instrument.highestNote > instrument.lowestNote
    )
      for (
        let note = instrument.lowestNote;
        note < instrument.highestNote + 1;
        note++
      ) {
        this.range.push(note);
      }

    // if (this.range.length == 0) for (let i = 0; i < 88; i++) this.range.push(i);
  }

  notify(messageType: number, _message: string, _messageValue: any) {
    // console.log("NOTIFIED")
    if (messageType == Constants.TICKER_UPDATED) {
      this.updateDisplay();
      console.log("TICKER_UPDATED")
    }

    if (messageType == Constants.INSTRUMENT_SELECTED) {
      if (this.instruments == undefined)
        return

      console.log("INSTRUMENT_SELECTED")
      this.instruments
        .filter((instrument) => instrument.id == _messageValue)
        .forEach((instrument) => this.onInstrumentSelected(instrument));
      this.updateDisplay();
    }

    if (messageType == Constants.BEAT_DIV) {
      console.log("BEAT_DIV")
      this.beatIndicators = [];
      for (let i = 0; i < this.beats.length; i++)
        this.beatIndicators.push(i + 1 == _messageValue);
    }
    if (messageType == Constants.BAR_DIV) {
      console.log("BAR_DIV")
      this.barIndicators = [];
      for (let i = 0; i < this.bars.length; i++)
        this.barIndicators.push(i + 1 == _messageValue);
    }
    if (messageType == Constants.PART_DIV) {
      console.log("PART_DIV")
      this.partIndicators = [];
      for (let i = 0; i < this.parts.length; i++)
        this.partIndicators.push(i + 1 == _messageValue);
    }
  }

  updateDisplay() {
    this.midiService.tickerInfo().subscribe((data) => {
      this.ticksPosition = this.colCount;

      this.ticker = data;
      this.ticks = [];
      this.beats = [];
      this.bars = [];
      this.divs = [];
      this.parts = [];

      this.beatIndicators = [];
      // this.selectedTicks = []
      // this.selectedBeats = []
      // this.selectedBars = []
      // this.selectedDivs = []
      // this.selectedParts = []

      this.tickRange = [];
      this.tickOverflow = [];

      for (let index = 0; index < this.ticker.ticksPerBeat; index++) {
        this.ticks.push(index + 1);
        this.selectedTicks.push(false);
        if (this.tickRange.length < this.colCount) this.tickRange.push(index);
      }

      while (this.tickRange.length + this.tickOverflow.length < this.colCount)
        this.tickOverflow.push('');

      for (let index = 0; index < this.ticker.beatsPerBar; index++) {
        this.beats.push(index + 1);
        this.selectedBeats.push(false);
        this.beatIndicators.push(false);
      }

      for (let index = 0; index < this.divCount; index++) {
        this.divs.push(index + 1);
        this.selectedDivs.push(false);
      }

      for (let index = 0; index < this.ticker.bars; index++) {
        this.bars.push(index + 1);
        this.selectedBars.push(false);
      }

      for (let index = 0; index < this.ticker.parts; index++) {
        this.parts.push(index + 1);
        this.selectedParts.push(false);
      }
    });

    // this.beats.forEach(() => this.beatIndicators.push(false));
  }

  toggleInterval(interval: number, data: number[], resolution: string) {
    let num = 0;
    data.forEach((_t) => {
      if (num % interval == 0)
        this.uiService.notifyAll(Constants.CLICK, resolution, num);
      num++;
    });

    this.updateDisplay();
  }

  // updateSelections() {
  //   let index = 0
  //   this.selectedTicks.forEach(t => {
  //     let name = "mini-tick-btn-" + index
  //     let element = document.getElementById(name)
  //     if (element != undefined)
  //       this.uiService.addClass(element, "mini-btn-selected")
  //   })
  // }

  onNoteClicked(note: number, event: Event) {
    this.range.forEach((_note) => {
      let element = document.getElementById(
        'mini-note-btn-' + this.selectedNote
      );
      if (element != undefined)
        this.uiService.removeClass(element, '-selected');
    });

    this.selectedNote = note;
    this.uiService.swapClass(event.target, 'mini-btn-selected', 'mini-btn');
  }

  getNoteButtonClass(value: number): string {
    let note = this.getNote(value);
    return note.includes('♯') || note.includes('♯')
      ? ' piano-btn bg-gray'
      : 'piano-btn bg-lightgray';
  }

  isDrumMachine() {
    return this.selectedInstrument.lowestNote > 0 && this.selectedInstrument.highestNote > this.selectedInstrument.lowestNote;
  }

  getNote(value: number): string {
    return this.uiService.getNoteForValue(value, Constants.SCALE_NOTES);
  }

  getAccentsAsStrings(): string[] {
    let accents: string[] = [];
    this.ticks.forEach((_t) => accents.push('𝄈'));
    return accents;
  }

  getTickRangeAsStrings() {
    return this.tickRange.map((tr) => String(tr));
  }

  getTicksAsStrings(): string[] {
    return this.ticks.map(String);
  }

  getDivsAsStrings(): string[] {
    return this.divs.map(String);
  }

  getBeatsAsStrings(): string[] {
    return this.beats.map(String);
  }

  getBarsAsStrings(): string[] {
    return this.bars.map(String);
  }

  getPartsAsStrings(): string[] {
    return this.parts.map(String);
  }

  tickSelected(index: number) {
    this.selectedTicks[index] = !this.selectedTicks[index];
  }

  divSelected(index: number) {
    this.selectedDivs[index] = !this.selectedDivs[index];
  }

  commandSelected(command: string, level: number) {
    alert(command + ' ' + this.resolution[level]);
    // this.selectedTicks[index] = !this.selectedTicks[index];
  }

  beatSelected(index: number) {
    this.selectedBeats[index] = !this.selectedBeats[index];
  }

  barSelected(index: number) {
    this.selectedBars[index] = !this.selectedBars[index];
  }

  partSelected(index: number) {
    this.selectedParts[index] = !this.selectedParts[index];
  }

  toggleAll() {
    let num = 0;
    this.ticks.forEach((_t) =>
      this.uiService.notifyAll(Constants.CLICK, this.resolution[1], num++)
    );
    this.updateDisplay();
  }

  clearAll(data: number[]) {
    data.forEach((_t) => {
      this.uiService.notifyAll(Constants.CLICK, '0', 0);
    });

    this.updateDisplay();
  }

  nudgeRight(resolution: string) {
    this.uiService.notifyAll(Constants.NUDGE_RIGHT, resolution, 0);
  }
}

// toggleThrees() {
//   let num = 0;
//   this.ticks.forEach(t => {
//     if (num % 3 == 0)
//       this.uiService.notifyAll(Constants.BTN_SELECTION, this.resolution[1], num)
//     num++
//   })

//   this.updateDisplay()
// }

// toggleFours() {
//   let num = 0;
//   this.ticks.forEach(t => {
//     if (num % 4 == 0)
//       this.uiService.notifyAll(Constants.BTN_SELECTION, this.resolution[1], num)
//     num++
//   })

//   this.updateDisplay()
// }
