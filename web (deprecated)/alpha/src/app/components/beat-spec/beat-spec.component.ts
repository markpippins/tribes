import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Pattern } from 'src/app/models/pattern';
import { PatternUpdateType } from 'src/app/models/pattern-update-type';
import { Song } from 'src/app/models/song';
import { Step } from 'src/app/models/step';
import { Swirl } from 'src/app/models/swirl';
import { TickListener } from 'src/app/models/tick-listener';
import { TickerStatus } from 'src/app/models/ticker-status';
import { TickerService } from 'src/app/services/ticker.service';
import { UiService } from 'src/app/services/ui.service';
import { MidiService } from '../../services/midi.service';
import { MessageListener } from 'src/app/models/message-listener';


@Component({
  selector: 'app-beat-spec',
  templateUrl: './beat-spec.component.html',
  styleUrls: ['./beat-spec.component.css'],
})
export class BeatSpecComponent implements OnInit, OnDestroy, MessageListener, TickListener {
  editStep: number | undefined;
  tickerId!: number;
  song!: Song;
  tickerSubscription!: Subscription;
  rows: string[][] = [
    [
      'Ride',
      'Clap',
      'Perc',
      'Bass',
      'Tom',
      'Clap',
      'Wood',
      'P1',
      'Ride',
      'fx',
      'Perc',
      'Bass',
      'Kick',
      'Snare',
      'Closed Hat',
      'Open Hat',
    ],
  ];

  pulse = 0;

  swirls = new Swirl<boolean>([
    true,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
    false,
  ]);

  instruments!: Instrument[];

  private subscriptions: Subscription[] = [];

  private forward = true;
  private count = 0;

  constructor(private tickerService: TickerService, private midiService: MidiService, private uiService: UiService) {

  }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
    this.tickerService.removeListener(this);
    // Clean up all subscriptions
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  ngOnInit(): void {
    this.uiService.addListener(this);
    this.tickerService.addListener(this);

    // Store subscriptions for cleanup
    this.subscriptions.push(
      this.midiService.instruments$.subscribe(instruments => {
        if (instruments.length > 0) {
          this.instruments = this.uiService.sortByName(instruments);
        }
      })
    );

    this.subscriptions.push(
      this.midiService.song$.subscribe(song => {
        if (song) {
          this.song = song;
          this.song.patterns = this.uiService.sortByPosition(this.song.patterns);
          this.song.patterns.forEach(p =>
            p.steps = this.uiService.sortByPosition(p.steps)
          );
        }
      })
    );
  }

  notify(messageType: number, _message: string, messageValue: number) {
    // console.log("NOTIFIED")
    if (messageType == Constants.TICKER_SELECTED) {
      this.tickerId = messageValue;
      console.log("TICKER_SELECTED")
    }
  }

  update(_tickerStatus: TickerStatus): void {

    // if (this.pulse == tickerStatus.tick)
    //   return

    this.pulse++;

    if (this.pulse % 8 == 0) {
      this.count++;

      if (this.forward) this.swirls.forward();
      else this.swirls.reverse();

      if (this.count % 15 == 0) this.forward = !this.forward;

    }
  }

  paramsBtnClicked(step: number) {
    this.editStep = step;
  }

  onInstrumentSelected(instrument: Instrument, pattern: Pattern) {
    this.midiService
      .updatePattern(
        pattern.id,
        PatternUpdateType.INSTRUMENT,
        instrument.channel
      )
      .subscribe((data) => (this.song.patterns[pattern.position] = data));
  }

  selectedIndexChange(index: number) {
    this.uiService.notifyAll(
      Constants.INSTRUMENT_SELECTED,
      '',
      this.song.patterns[index].instrument?.id
    );
  }

  onStepChanged(_step: any) {
    // this.midiService.updateStep(step).subscribe(async data => {
    //   this.steps[step.position] = data
    // })
  }

  getInstrumentForStep(_pattern: Pattern, _step: Step): Instrument {
    let result = this.instruments?.filter(
      (i) => i.id == _pattern?.instrument?.id
    );
    return result[0];
  }

  getLabel(pattern: Pattern): string {
    let s = 'XOX ' + pattern.channel;
    if (pattern.name != undefined) s = pattern.name;

    if (s.toLowerCase() == 'microsoft gs wavetable synth') s = 'MS Wave';

    if (s.toLowerCase() == 'gervill') s = 'Gervill';

    return s;
    // return s + ' [' + pattern.position + ']';
  }
}
