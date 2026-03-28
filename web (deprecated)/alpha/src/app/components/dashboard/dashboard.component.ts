import { Component, OnDestroy, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { TickerStatus } from 'src/app/models/ticker-status';
import { TickerUpdateType } from 'src/app/models/ticker-update-type';
import { UiService } from 'src/app/services/ui.service';
import { Player } from '../../models/player';
import { Ticker } from '../../models/ticker';
import { MidiService } from '../../services/midi.service';
import { MessageListener } from 'src/app/models/message-listener';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnDestroy, OnInit, MessageListener {
  @Output()
  players!: Player[];

  @Output()
  selectedPlayer!: Player;

  playing = false;

  activeInstrument!: Instrument;

  @Output()
  instruments!: Instrument[];

  @Output()
  selectedInstrument!: Instrument;

  @Output()
  tickerStatus: TickerStatus = {
    id: 0,
    bars: 0,
    beatsPerBar: 0,
    beatDivider: 0,
    partLength: 0,
    maxTracks: 0,
    songLength: 0,
    swing: 0,
    ticksPerBeat: 0,
    tempoInBPM: 0,
    loopCount: 0,
    parts: 0,
    noteOffset: 0,
    playing: false,
    tick: 0,
    beat: 0,
    bar: 0,
    part: 0,
    tickCount: 0,
    beatCount: 0,
    barCount: 0,
    partCount: 0,
    patternStatuses: [],
    playerCount: 0,
    hasSolos: false
  };

  ticker: Ticker = {
    bar: 0,
    beat: 0,
    beatDivider: 0,
    beatsPerBar: 0,
    done: false,
    id: 0,
    maxTracks: 0,
    partLength: 4,
    playing: false,
    songLength: 0,
    stopped: false,
    swing: 0,
    tempoInBPM: 0,
    tick: 0,
    ticksPerBeat: 0,
    activePlayerIds: [],
    part: 0,
    bars: 16,
    parts: 4,
    beats: 0,
    barCount: 0,
    beatCount: 0,
    tickCount: 0,
    partCount: 0,
    noteOffset: 0,
    players: [],
  };

  running = false;

  @Output()
  consoleOutput: string[] = [];

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }

  ngOnInit(): void {
    this.uiService.addListener(this);
    // this.onActionSelected('forward');
    this.midiService.allInstruments().subscribe((data) => {
      this.instruments = this.uiService.sortByName(data);
      if (this.instruments.length > 0) {
        this.selectedInstrument = this.instruments[0];
      }
    });
    this.updateDisplay();

  }

  notify(_messageType: number, _message: string, _messageValue: any) {
    console.log("NOTIFICATION:  " + _message);
    this.consoleOutput.pop();
    switch (_messageType) {
      case Constants.STATUS:
        console.log("STATUS:  " + _message);
        this.consoleOutput.push(_message);
        break;

      case Constants.COMMAND:
        console.log("COMMAND:  " + _message);
        this.onActionSelected(_message);
        break;

      case Constants.CONNECTED:
        console.log("CONNECTED:  " + _message);
        this.consoleOutput.push('connected');
        this.updateDisplay();
        break;

      case Constants.DISCONNECTED:
        console.log("DISCONNECTED:  " + _message);
        this.consoleOutput.push('disconnected');
        break;

      case Constants.PLAYER_UPDATED:
        this.consoleOutput.push(_message);
        this.updateDisplay();
        break;

      case Constants.TICKER_UPDATED:
        this.updateDisplay();
        break;

      case Constants.BEAT_DIV:
        this.tickerStatus = _messageValue;
        this.updateDisplay();
        break;

      case Constants.INSTRUMENT_SELECTED: {
        let instrument = this.instruments.filter(
          (instrument) => instrument.id == _messageValue
        );
        if (instrument.length > 0) {
          this.selectedInstrument = instrument[0];
        }
      }
    }
  }

  onActionSelected(action: string) {
    this.consoleOutput.pop();
    this.consoleOutput.push(action);

    console.log('handling ' + action);
    if (this.ticker != undefined)
      switch (action) {
        case 'ticker-forward': {
          if (this.ticker.id > 0 && this.ticker.playing) {
            this.consoleOutput.pop();
            this.consoleOutput.push('ticker is currently playing');
          } else
            this.midiService.next(this.ticker.id).subscribe(async (data) => {
              this.clear();
              this.ticker = data;
              this.uiService.notifyAll(Constants.TICKER_SELECTED, '', 0);

              this.midiService.playerInfo().subscribe((data) => {
                this.players = this.sortByChannel(data);
                if (this.players.length > 0)
                  this.selectedPlayer = this.players[0];
              });
            });
          this.uiService.notifyAll(
            Constants.TICKER_SELECTED,
            this.ticker.id.toString(),
            0
          );
          break;
        }

        case 'ticker-previous': {
          if (this.ticker != undefined && this.ticker.id > 0) {
            this.midiService
              .previous(this.ticker.id)
              .subscribe(async (data) => {
                this.clear();
                this.ticker = data;
                // this.updateTickerStatus()
                this.uiService.notifyAll(Constants.TICKER_SELECTED, '', 0);

                this.midiService.playerInfo().subscribe((data) => {
                  this.players = this.sortByChannel(data);
                  this.sortByPitch(this.players);
                  if (this.players.length > 0)
                    this.selectedPlayer = this.players[0];
                });
              });
          }
          this.uiService.notifyAll(
            Constants.TICKER_SELECTED,
            this.ticker.id.toString(),
            0
          );
          break;
        }

        case 'ticker-play': {
          this.playing = true
          this.uiService.notifyAll(
            Constants.TICKER_STARTED,
            this.ticker.id.toString(),
            0
          );
          this.midiService.start().subscribe();
          this.updateDisplay();

          break;
        }

        case 'ticker-stop': {
          this.playing = false
          this.midiService.stop().subscribe((data) => {
            this.ticker = data;
            this.uiService.notifyAll(Constants.TICKER_STOPPED, '', 0);
          });

          this.updateDisplay();
          break;
        }

        case 'ticker-pause': {
          this.midiService.pause().subscribe();
          // this.isPlaying = false
          // this.players = []
          // this.playerConditions = []
          break;
        }

        case 'ticker-record': {
          this.midiService.record().subscribe();
          // this.players = []
          // this.playerConditions = []
          break;
        }

        case 'ticker-add': {
          if (this.selectedInstrument)
            this.midiService
              .addPlayer(this.selectedInstrument.name)
              .subscribe(async (data) => {
                this.players.push(data);
                this.selectedPlayer = data;
              });
          break;
        }

        case 'ticker-remove': {
          if (this.selectedPlayer == undefined) break;
          let id = this.selectedPlayer.id;
          this.selectedPlayer.rules = [];
          this.midiService
            .removePlayer(this.selectedPlayer)
            .subscribe(async (data) => {
              this.players = this.sortByChannel(data);
              // if (this.players.length == 0) this.selectedPlayer = undefined;
            });
          this.players = this.players.filter((p) => p.id != id);

          if (this.players.length > 0)
            this.selectedPlayer = this.players[this.players.length - 1];
          break;
        }

        case 'ticker-refresh': {
          this.updateDisplay();
          break;
        }

        case 'ticker-transpose-up': {
          this.midiService
            .updateTicker(this.ticker.id, TickerUpdateType.BASE_NOTE_OFFSET, 1)
            .subscribe();
          break;
        }

        case 'ticker-transpose-down': {
          this.midiService
            .updateTicker(this.ticker.id, TickerUpdateType.BASE_NOTE_OFFSET, -1)
            .subscribe();
          break;
        }

        case 'save': {
          this.midiService.saveConfig().subscribe();
          break;
        }

        case 'ticker-clear': {
          this.midiService.clearPlayers().subscribe();
          this.clear();
          break;
        }
      }
      else console.error('TICKER UNDEFINED')

      // this.updateDisplay();
  }

  updateDisplay(): void {
    // window.addEventListener('load', function (e) {});

    if (this.ticker.id == 0) {
      this.midiService.next(0).subscribe((_data) => {
        this.ticker = _data;
        this.players = this.sortByChannel(_data.players); // data)
      });
    }

    else if (!this.playing)
      this.midiService.playerInfo().subscribe(async (data) => {
        this.players = this.sortByChannel(data);
      });

    else if (this.playing)
      this.midiService.playerInfo().subscribe(async () => {
        this.players.forEach(
          (p) => (p.active = p.id in this.ticker.activePlayerIds)
        );
      });
  }

  onPlayerSelected(player: Player) {
    this.selectedPlayer = player;
    this.uiService.notifyAll(
      Constants.PLAYER_SELECTED,
      'Player selected',
      player.id
    );
  }

  onRuleChange(_player: Player) {
    // this.ruleChangeEvent.emit(player);
  }

  clear() {
    // this.selectedPlayer = undefined;
  }

  sortByPitch(data: Player[]): any[] {
    return data.sort((a, b) => {
      if (a.note > b.note) {
        return 1;
      }
      if (a.note < b.note) {
        return -1;
      }
      return 0;
    });
  }

  reverseSortByClass(data: Player[]): any[] {
    return data.sort((b, a) => {
      if (a.playerClass > b.playerClass) {
        return 1;
      }
      if (a.playerClass < b.playerClass) {
        return -1;
      }
      return 0;
    });
  }

  sortByChannel(data: Player[]): any[] {
    return data.sort((b, a) => {
      if (a.channel + 1 > b.channel) {
        return -1;
      }
      if (a.channel < b.channel) {
        return 1;
      }
      return 0;
    });
  }
}
