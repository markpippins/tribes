import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Subscription } from 'rxjs';
import { Swirl } from 'src/app/models/swirl';
import { TickListener } from 'src/app/models/tick-listener';
import { TickerStatus } from 'src/app/models/ticker-status';
import { TickerService } from 'src/app/services/ticker.service';
import { Instrument } from '../../../models/instrument';

@Component({
  selector: 'app-drum-pad',
  templateUrl: './drum-pad.component.html',
  styleUrls: ['./drum-pad.component.css'],
})
export class DrumPadComponent implements TickListener, OnDestroy, OnInit {

  tickerSubscription!: Subscription;

  @Output()
  padPressedEvent = new EventEmitter<number>();

  @Input()
  name!: string;

  @Input()
  caption!: string;

  @Input()
  swirling = false;
  swirl = new Swirl<number>([0, 1, 2, 3, 4, 5, 6, 7]);

  @Input()
  selector: boolean = false;

  @Input()
  index!: number;

  @Input()
  note!: number;

  @Input()
  otherNote!: number;

  @Input()
  instrument!: Instrument;

  @Input()
  pressed!: boolean;

  @Input()
  active: boolean = false;

  @Input()
  muted: boolean = false;

  @Input()
  channel!: number;

  pulse = 0;


  constructor(private tickerService: TickerService) { }

  ngOnDestroy(): void {
    // this.uiService.removeListener(this);
    this.tickerService.removeListener(this);
  }

  ngOnInit(): void {
    // this.uiService.addListener(this);
    this.tickerService.addListener(this);
  }

  update(_tickerStatus: TickerStatus): void {
    this.pulse++;
    this.swirl.forward();
  }

  padPressed() {
    this.padPressedEvent.emit(this.index);
  }

  getPadClass(): string {
    let result = this.pressed
      ? 'pad pad-' + this.index + ' pressed'
      : 'pad pad-' + this.index;

    return result;
  }

  getPadRingClass(): string {
    if (this.selector) return 'none';

    let result = this.active ? 'pad-ring-active' : 'pad-ring';

    return result;
  }

  getIndicatorClass(index: number): string {
    if (this.swirling) return 'pad-' + (this.swirl.getItems()[index] + 3);

    let result = 'pad-indicator-content';

    switch (index) {
      case 0: {
        result += this.pressed ? ' armed' : ' mute';
        break;
      }
      case 1: {
        result += this.muted ? ' muted' : ' active';
        break;
      }
    }

    return result;
  }
}
