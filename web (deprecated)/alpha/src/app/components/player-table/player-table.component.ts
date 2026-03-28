import { AfterContentChecked, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { MidiMessage } from 'src/app/models/midi-message';
import { PlayerUpdateType } from 'src/app/models/player-update-type';
import { UiService } from 'src/app/services/ui.service';
import { Player } from '../../models/player';
import { MidiService } from '../../services/midi.service';

@Component({
  selector: 'app-player-table',
  templateUrl: './player-table.component.html',
  styleUrls: ['./player-table.component.css'],
})
export class PlayerTableComponent implements OnInit, AfterContentChecked {

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  @Output()
  playerAddedEvent = new EventEmitter();

  @Output()
  ruleChangeEvent = new EventEmitter<Player>();

  selectedPlayer!: Player

  @Output()
  instruments!: Instrument[];

  @Input()
  players!: Player[];

  playerCols = Constants.PLAYER_COLUMNS;

  constructor(private midiService: MidiService, private uiService: UiService) { }

  ngAfterContentChecked(): void {
    this.midiService.allInstruments().subscribe((data) => {
      this.instruments = this.uiService.sortByName(data);
    });
  }

  ngOnInit(): void {
    //
  }

  onRowClick(player: Player) {
    this.selectedPlayer = player
    this.playerSelectEvent.emit(player);
  }

  onBtnClick(player: Player, action: string) {
    console.log(action)

    switch (action) {

      case 'ticker-add': {
        console.log("ticker-add")
        this.midiService.addPlayer("Gervill").subscribe(async (data) => {
          this.players.push(data);

          this.midiService
            .addRule(this.players[this.players.length - 1])
            .subscribe(async (data) => {
              this.players[this.players.length - 1].rules.push(data);
              this.ruleChangeEvent.emit(this.players[this.players.length - 1]);
            });
        });
        break;
      }

      case 'ticker-remove': {
        console.log("ticker-remove")
        player.rules = [];
        this.midiService.removePlayer(player).subscribe(async (data) => {
          this.players = data;
        });
        this.players = this.players.filter((p) => p.id != player.id);
        break;
      }

      case 'player-mute': {
        this.midiService.sendMessage(
          player.instrumentId,
          player.channel,
          MidiMessage.NOTE_OFF,
          0, 0
        );

        let index = this.players.indexOf(player);
        this.players[index].muted = !this.players[index].muted;
        this.midiService
          .updatePlayer(
            this.players[index].id,
            PlayerUpdateType.MUTE,
            this.players[index].muted ? 1 : 0
          )
          .subscribe((data) => (this.players[index] = data));
        break;
      }

      case 'player-solo': {

        player.solo = !player.solo

        let index = this.players.indexOf(player);

        this.midiService
          .updatePlayer(
            player.id,
            PlayerUpdateType.SOLO,
            player.solo ? 1 : 0
          )
          .subscribe(data => {
            this.players[index] = data;
            this.players[index].solo = player.solo
          });

        break;
      }

      case 'ticker-audition': {
        // if (this.selectedPlayer != undefined) {
        console.log('auditioning player: ' + player.name);
        console.log('auditioning player instrument: ' + player.instrumentId);

        this.midiService.sendMessage(
          player.instrumentId,
          player.channel,
          MidiMessage.NOTE_ON,
          player.note,
          120
        );

        this.midiService.sendMessage(
          player.instrumentId,
          player.channel,
          MidiMessage.NOTE_OFF,
          player.note,
          120
        );
      }
        break;
    }
  }

  initBtnClicked() {
    this.onBtnClick(
      {
        id: 0,
        tickerId: 0,
        instrumentId: 0,
        maxVelocity: 0,
        minVelocity: 0,
        note: 0,
        preset: 0,
        probability: 0,
        rules: [],
        allowedControlMessages: [],
        parts: [],
        muted: false,
        name: '',
        channel: 0,
        swing: 0,
        level: 0,
        active: false,
        ratchetCount: 0,
        ratchetInterval: 0,
        skips: 0,
        beatFraction: 0,
        subDivisions: 0,
        playerClass: '',
        randomDegree: 0,
        fadeIn: 0,
        fadeOut: 0,
        solo: false
      },
      'add'
    );
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    console.log("onInstrumentSelected")
    player.instrumentId = instrument.id;
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.INSTRUMENT, instrument.id)
      .subscribe((data) => {
        if (data.instrumentId == player.instrumentId)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onNoteChange(player: Player, event: { target: any }) {
    console.log("onNoteChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.NOTE, event.target.value)
      .subscribe((data) => {
        if (data.note == player.note)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onPartChange(player: Player, event: { target: any }) {
    console.log("onPartChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.PART, event.target.value)
      .subscribe((data) => {
        // if (data.parts == player.parts)
        this.players[this.players.indexOf(player)] = data;
      });
  }

  onChannelChange(player: Player, event: { target: any }) {
    console.log("onChannelChange")
    this.players
      .filter((p) => p.id == player.id)
      .forEach((p) =>
        this.midiService
          .updatePlayer(p.id, PlayerUpdateType.CHANNEL, event.target.value)
          .subscribe()
      );
    this.uiService.notifyAll(Constants.PLAYER_UPDATED, 'Player updated', 0);
  }

  onPresetChange(player: Player, event: { target: any }) {
    console.log("onPresetChange")

    this.midiService.sendMessage(
      player.instrumentId,
      player.channel,
      MidiMessage.NOTE_OFF,
      player.note,
      120
    );

    this.players
      .filter((p) => p.instrumentId == player.instrumentId && p.channel == player.channel)
      .forEach((p) =>
        this.midiService
          .updatePlayer(p.id, PlayerUpdateType.PRESET, event.target.value)
          .subscribe()

      );
    this.uiService.notifyAll(Constants.PLAYER_UPDATED, 'Player updated', 0);
  }

  onLevelChange(player: Player, event: { target: any }) {
    console.log("onLevelChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.LEVEL, event.target.value)
      .subscribe((data) => {
        if (data.level == player.level)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onMinVelocityChange(player: Player, event: { target: any }) {
    console.log("onMinVelocityChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.MIN_VELOCITY,
        event.target.value
      )
      .subscribe((data) => {
        if (data.minVelocity == player.minVelocity)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onMaxVelocityChange(player: Player, event: { target: any }) {
    console.log("onMaxVelocityChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.MAX_VELOCITY,
        event.target.value
      )
      .subscribe((data) => {
        if (data.maxVelocity == player.maxVelocity)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onRatchetCountChange(player: Player, event: { target: any }) {
    console.log("onRatchetCountChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.RATCHET_COUNT,
        event.target.value
      )
      .subscribe((data) => {
        if (data.ratchetCount == player.ratchetCount)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onRatchetIntervalChange(player: Player, event: { target: any }) {
    console.log("onRatchetIntervalChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.RATCHET_INTERVAL,
        event.target.value
      )
      .subscribe((data) => {
        if (data.ratchetInterval == player.ratchetInterval)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onFadeInChange(player: Player, event: { target: any }) {
    console.log("onFadeInChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.FADE_IN, event.target.value)
      .subscribe((data) => {
        if (data.fadeIn == player.fadeIn)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onFadeOutChange(player: Player, event: { target: any }) {
    console.log("onFadeOutChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.FADE_OUT, event.target.value)
      .subscribe((data) => {
        if (data.fadeOut == player.fadeOut)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onProbabilityChange(player: Player, event: { target: any }) {
    console.log("onProbabilityChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.PROBABILITY, event.target.value)
      .subscribe((data) => {
        if (data.probability == player.probability)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onSwingChange(player: Player, event: { target: any }) {
    console.log("onSwingChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.SWING, event.target.value)
      .subscribe((data) => {
        if (data.swing == player.swing)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onBeatFractionChange(player: Player, event: { target: any }) {
    console.log("onBeatFractionChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.BEAT_FRACTION,
        event.target.value
      )
      .subscribe((data) => {
        if (data.beatFraction == player.beatFraction)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onRandomDegreeChange(player: Player, event: { target: any }) {
    console.log("onRandomDegreeChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.RANDOM_DEGREE,
        event.target.value
      )
      .subscribe((data) => {
        if (data.randomDegree == player.randomDegree)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onSkipsChange(player: Player, event: { target: any }) {
    console.log("onSkipsChange")
    this.midiService
      .updatePlayer(player.id, PlayerUpdateType.SKIPS, event.target.value)
      .subscribe((data) => {
        if (data.skips == player.skips)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onSubsChange(player: Player, event: { target: any }) {
    console.log("onSubsChange")
    this.midiService
      .updatePlayer(
        player.id,
        PlayerUpdateType.SUBDIVISIONS,
        event.target.value
      )
      .subscribe((data) => {
        if (data.subDivisions == player.subDivisions)
          this.players[this.players.indexOf(player)] = data;
      });
  }

  onPass(player: Player) {
    if (player != undefined && this.selectedPlayer == undefined)
      this.playerSelectEvent.emit(player);
  }

  // getMuteButtonClass(player: Player): string {
  //   return player.muted ? 'muted' : 'unmuted';
  // }

  getRowClass(player: Player): string {
    return 'table-row' + (player === this.selectedPlayer ? ' selected' : '');
  }

  onClick(command: string) {

    switch (command) {
      case 'ticker-add':
        this.playerAddedEvent.emit()
        break
    }

    // this.http.get('http://localhost:8080/api/' + command).subscribe(
    //   (response) => {
    //     console.log(response);
    //   },
    //   (error) => {
    //     console.error('Error:', error);
    //   }
    // );
  }
}
