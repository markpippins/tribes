import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Player } from '../models/player';
import { Instrument } from '../models/instrument';
import { Ticker } from '../models/ticker';
import { Evaluator } from '../models/evaluator';
import { Rule } from "../models/rule";
import { Step } from "../models/step";
import { LookupItem } from "../models/lookup-item";
import { Device } from "../models/device";
import { Song } from '../models/song';
import { Pattern } from '../models/pattern';
import { TickerStatus } from '../models/ticker-status';
import { BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { shareReplay } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class MidiService {

  // Add private BehaviorSubjects for caching
  private songSubject = new BehaviorSubject<Song | null>(null);
  private instrumentsSubject = new BehaviorSubject<Instrument[]>([]);
  private playersSubject = new BehaviorSubject<Player[]>([]);
  private tickerStatusSubject = new BehaviorSubject<TickerStatus | null>(null);

  // Public Observables for components to subscribe to
  readonly song$ = this.songSubject.asObservable();
  readonly instruments$ = this.instrumentsSubject.asObservable();
  readonly players$ = this.playersSubject.asObservable();
  readonly tickerStatus$ = this.tickerStatusSubject.asObservable();

  // Cache refresh intervals
  private readonly REFRESH_INTERVAL = 100000; // 100 seconds

  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      responseType: 'text',
    }),
  };

  URL!: string;

  constructor(public http: HttpClient) {
    this.URL = 'http://localhost:8080/api';
    this.setupRefreshIntervals();
    this.initialLoad();
  }

  private setupRefreshIntervals() {
    setInterval(() => this.refreshTickerStatus(), this.REFRESH_INTERVAL);
    setInterval(() => this.refreshSong(), this.REFRESH_INTERVAL * 5);
  }

  private async initialLoad() {
    await this.refreshSong();
    await this.refreshInstruments();
    await this.refreshPlayers();
    await this.refreshTickerStatus();
  }

  // Refresh methods
  private refreshSong() {
    return this.http.get<Song>('http://localhost:8080/api/song/info')
      .subscribe(song => this.songSubject.next(song));
  }

  private refreshInstruments() {
    return this.http.get<Instrument[]>('http://localhost:8080/api/instruments/all')
      .subscribe(instruments => this.instrumentsSubject.next(instruments));
  }

  private refreshPlayers() {
    return this.http.get<Player[]>('http://localhost:8080/api/players/info')
      .subscribe(players => this.playersSubject.next(players));
  }

  private refreshTickerStatus() {
    return this.http.get<TickerStatus>('http://localhost:8080/api/ticker/status')
      .subscribe(status => this.tickerStatusSubject.next(status));
  }

  delay(ms: number) {
    console.log('delay');
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  start() {
    console.log('start');
    return this.http.get('http://localhost:8080/api/ticker/start');
  }

  next(currentTickerId: number) {
    console.log('next');
    let params = new HttpParams();
    params = params.append('currentTickerId', currentTickerId);
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/next', { params });
  }

  previous(currentTickerId: number) {
    console.log('previous');
    let params = new HttpParams();
    params = params.append('currentTickerId', currentTickerId);
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/previous', { params });
  }

  stop() {
    console.log('stop');
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/stop');
  }

  pause() {
    console.log('pause');
    return this.http.get('http://localhost:8080/api/ticker/pause');
  }

  playerInfo() {
    console.log('playerInfo');
    return this.http.get<Player[]>('http://localhost:8080/api/players/info');
  }

  songInfo() {
    // Only make HTTP request if we don't have cached data
    if (!this.songSubject.value) {
      this.http.get<Song>('http://localhost:8080/api/song/info')
        .pipe(
          shareReplay(1)
        )
        .subscribe(song => this.songSubject.next(song));
    }
    return this.song$;
  }

  tickerInfo() {
    console.log('tickerInfo');
    return this.http.get<Ticker>('http://localhost:8080/api/ticker/info');
  }

  tickerStatus() {
    console.log('tickerStatus');
    return this.http.get<TickerStatus>('http://localhost:8080/api/ticker/status');
  }

  allDevices() {
    console.log('allDevices');
    return this.http.get<Device[]>(
      'http://localhost:8080/api/devices/info');
  }

  instrumentInfoByChannel(device: string, channel: number) {
    console.log('instrumentInfoByChannel');
    let params = new HttpParams();
    params = params.append('channel', channel);
    params = params.append('device', device);
    return this.http.get<Instrument>(
      'http://localhost:8080/api/midi/instrument',
      { params: params }
    );
  }

  // instrumentInfoByName(name: string) {
  //   let params = new HttpParams();
  //   params = params.append('name', name);
  //   return this.http.get<Instrument>(
  //     'http://localhost:8080/api/midi/instrument',
  //     { params: params }
  //   );
  // }

  instrumentInfoById(instrumentId: number) {
    console.log('instrumentInfoById');
    let params = new HttpParams();
    params = params.append('instrumentId', instrumentId);
    return this.http.get<Instrument>(
      'http://localhost:8080/api/instrument',
      { params: params }
    );
  }

  // Update BehaviorSubjects to track loading state
  private isLoadingInstruments = false;
  private lastInstrumentsLoad = 0;
  private readonly CACHE_DURATION = 60000; // 1 minute cache

  allInstruments() {
    // Check if we have cached data and it's still fresh
    if (this.instrumentsSubject.value.length > 0 &&
        Date.now() - this.lastInstrumentsLoad < this.CACHE_DURATION) {
      return this.instruments$;
    }

    // Prevent multiple simultaneous requests
    if (!this.isLoadingInstruments) {
      this.isLoadingInstruments = true;

      this.http.get<Instrument[]>('http://localhost:8080/api/instruments/all')
        .pipe(
          shareReplay(1) // Share the response with all subscribers
        )
        .subscribe({
          next: (instruments) => {
            this.instrumentsSubject.next(instruments);
            this.lastInstrumentsLoad = Date.now();
          },
          complete: () => {
            this.isLoadingInstruments = false;
          }
        });
    }

    return this.instruments$;
  }

  instrumentLookup() {
    console.log('instrumentLookup');
    // let params = new HttpParams();
    return this.http.get<LookupItem[]>(
      'http://localhost:8080/api/instruments/lookup');
  }

  saveConfig() {
    console.log('saveConfig');
    // let params = new HttpParams();
    return this.http.get<LookupItem[]>(
      'http://localhost:8080/api/ticker/save');
  }

  clearPlayers() {
    console.log('clearPlayers');
    return this.http.get('http://localhost:8080/api/players/clear');
  }

  playNote(instrument: string, channel: number, note: number) {
    console.log('playNote');
    let params = new HttpParams();
    params = params.append('instrument', instrument);
    params = params.append('channel', channel);
    params = params.append('note', note);
    return this.http
      .get('http://localhost:8080/api/drums/note', { params: params })
      .subscribe();
  }

  sendMessage(
    instrumentId: number,
    channel: number,
    messageType: number,
    data1: number,
    data2: number
  ) {
    console.log('sendMessage');
    console.log('instrumentId: ' + instrumentId);
    console.log('channel: ' + channel);
    console.log('messageType: ' + messageType);

    let params = new HttpParams();
    params = params.append('instrumentId', instrumentId);
    params = params.append('channel', channel);
    params = params.append('messageType', messageType);
    params = params.append('data1', data1);
    params = params.append('data2', data2);
    return this.http
      .get('http://localhost:8080/api/messages/send', { params: params })
      .subscribe();
  }

  record() {
    console.log('record');
    return this.http.get('http://localhost:8080/api/players/clear');
  }

  addPlayer(instrument: string) {
    console.log('addPlayer');
    let params = new HttpParams();
    params = params.append('instrument', instrument);
    return this.http.get<Player>('http://localhost:8080/api/players/add', {
      params: params,
    });
  }

  addPlayerForNote(instrument: string, note: number) {
    console.log('addPlayerForNote');
    let params = new HttpParams();
    params = params.append('instrument', instrument);
    params = params.append('note', note);
    return this.http.get<Player>('http://localhost:8080/api/players/note', {
      params: params,
    });
  }

  newPlayer(instrumentId: number) {
    console.log('newPlayer');
    let params = new HttpParams();
    params = params.append('instrumentId', instrumentId);
    return this.http.get<Player>('http://localhost:8080/api/players/new', {
      params: params,
    });
  }

  removePlayer(player: Player) {
    console.log('removePlayer');
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get<Player[]>('http://localhost:8080/api/players/remove', {
      params: params,
    });
  }

  addRule(player: Player) {
    console.log('addRule');
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    return this.http.get<Rule>('http://localhost:8080/api/rules/add', {
      params: params,
    })
  }

  addSpecifiedRule(player: Player, operator: number, comparison: number, value: number, part: number) {
    console.log('addSpecifiedRule');
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    params = params.append('operator', operator);
    params = params.append('comparison', comparison);
    params = params.append('part', part);
    params = params.append('value', value);
    return this.http.get<Rule>('http://localhost:8080/api/rules/specify', {
      params: params,
    })
  }

  removeRule(player: Player, rule: Evaluator) {
    console.log('removeRule');
    let params = new HttpParams();
    params = params.append('playerId', player.id);
    params = params.append('ruleId', rule.id);
    return this.http.get('http://localhost:8080/api/rules/remove', {
      params: params,
    });
  }

  updatePlayer(playerId: number, updateType: number, updateValue: number) {
    console.log('updatePlayer');
    let params = new HttpParams();
    params = params.append('playerId', playerId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Player>('http://localhost:8080/api/player/update', {
      params: params,
    });
  }

  updatePattern(patternId: number, updateType: number, updateValue: number) {
    console.log('updatePattern');
    let params = new HttpParams();
    params = params.append('patternId', patternId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Pattern>('http://localhost:8080/api/patterns/update', {
      params: params,
    }).pipe(
      tap(() => this.refreshSong()) // Refresh song data after pattern update
    );
  }

  updateTicker(tickerId: number, updateType: number, updateValue: number) {
    console.log('updateTicker');
    let params = new HttpParams();
    params = params.append('tickerId', tickerId);
    params = params.append('updateType', updateType);

    params = updateValue != undefined ?
      params.append('updateValue', updateValue) :
      params = params.append('updateValue', 0);

    return this.http.get('http://localhost:8080/api/ticker/update', {
      params: params,
    });
  }


  // updateRule(playerId: number, ruleId: number, operator: number, comparison: number, newValue: number, part: number) {
  //   let params = new HttpParams();
  //   params = params.append('playerId', playerId);
  //   params = params.append('ruleId', ruleId);
  //   params = params.append('operator', operator);
  //   params = params.append('comparison', comparison);
  //   params = params.append('newValue', newValue);
  //   params = params.append('part', part);
  //   return this.http.get('http://localhost:8080/api/rule/update', {
  //     params: params,
  //   });
  // }

  updateRule(ruleId: number, updateType: number, updateValue: any) {
    console.log('updateRule');
    let params = new HttpParams();
    params = params.append('ruleId', ruleId);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Rule>('http://localhost:8080/api/rule/update', {
      params: params,
    });
  }

  refresh() {
    console.log('refresh');
    return this.http.get('http://localhost:8080/api/ticker/info');
  }

  addTrack(steps: Step[]) {
    console.log('addTrack');
    return this.http.post<Step[]>('http://localhost:8080/api/sequence/play', steps);
  }

  updateStep(stepId: number, position: number, updateType: number, updateValue: number) {
    console.log('updateStep');
    let params = new HttpParams();
    params = params.append('stepId', stepId);
    params = params.append('position', position);
    params = params.append('updateType', updateType);
    params = params.append('updateValue', updateValue);
    return this.http.get<Step>('http://localhost:8080/api/steps/update', {
      params: params,
    }).pipe(
      tap(() => this.refreshSong()) // Refresh song data after step update
    );
  }

  // Getter methods for current values
  getCurrentSong(): Song | null {
    return this.songSubject.value;
  }

  getCurrentInstruments(): Instrument[] {
    return this.instrumentsSubject.value;
  }

  getCurrentPlayers(): Player[] {
    return this.playersSubject.value;
  }

  getCurrentTickerStatus(): TickerStatus | null {
    return this.tickerStatusSubject.value;
  }
}
