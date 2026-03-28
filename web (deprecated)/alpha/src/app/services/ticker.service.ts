import { Injectable, NgZone, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Player } from '../models/player';
import { TickListener } from '../models/tick-listener';
import { Ticker } from '../models/ticker';
import { TickerStatus } from '../models/ticker-status';

@Injectable({
  providedIn: 'root'
})
export class TickerService implements OnInit {

  players: Player[] = []
  listeners: TickListener[] = [];

  ticker: Ticker = {
    bar: 0,
    beat: 0,
    beatDivider: 0,
    beatsPerBar: 0,
    done: false,
    id: 0,
    maxTracks: 0,
    partLength: 0,
    playing: false,
    songLength: 0,
    stopped: false,
    swing: 0,
    tempoInBPM: 0,
    tick: 0,
    ticksPerBeat: 0,
    activePlayerIds: [],
    part: 0,
    bars: 0,
    parts: 0,
    tickCount: 0,
    barCount: 0,
    beats: 0,
    beatCount: 0,
    partCount: 0,
    noteOffset: 0,
    players: []
  }

  constructor(private zone: NgZone) {
    this.getTickerMessages().subscribe(data => {
      let tickerStatus: TickerStatus = JSON.parse(data);
      this.listeners.forEach(listener => {
        listener.update(tickerStatus)
      })
    })
  }

  ngOnInit(): void {

  }

  getTicker(): Ticker {
    return this.ticker
  }

  addListener(listener: TickListener) {
    this.listeners.push(listener);
  }

  removeListener(listener: TickListener) {
    this.listeners = this.listeners.filter(l => l != listener);
  }

  getTickerMessages(): Observable<string> {
    return Observable.create(
      (observer: {
        next: (arg0: any) => void;
        error: (arg0: Event) => void;
      }) => {
        let source = new EventSource('http://localhost:8080/api/tick');
        source.onmessage = (event) => {
          this.zone.run(() => {
            observer.next(event.data);
          });
        };

        source.onerror = (event) => {
          this.zone.run(() => {
            observer.error(event);
          });
        };
      }
    );
  }


}
