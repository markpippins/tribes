import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { Player } from 'src/app/models/player';
import { PlayerUpdateType } from 'src/app/models/player-update-type';
import { MidiService } from 'src/app/services/midi.service';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-strike-row',
  templateUrl: './strike-row.component.html',
  styleUrls: ['./strike-row.component.css']
})
export class StrikeRowComponent {

  @Input()
  player!: Player

  @Output()
  playerSelectEvent = new EventEmitter<Player>();

  @Output()
  playerAddedEvent = new EventEmitter()

  @Output()
  playerRemovedEvent = new EventEmitter<Player>()

  playerCols: string[] = [
    // 'add',
    // 'remove',
    // 'mute',
    'ID',
    'InstrumentId',
    'Ch',
    'Device',
    'Pre',
    'Note',
    // 'operator',
    // 'comparison',
    // 'value',
    'Prob.',
    'VMin',
    'VMax',
  ];

  constructor(private midiService: MidiService, privateuiService: UiService) {
  }

  // onRowClick(player: Player, event: MouseEvent) {
  //   let element = document.getElementById("player-row-" + player.id)
  //   if (this.selectedPlayer == undefined) {
  //     this.selectedPlayer = player
  //     this.playerSelectEvent.emit(player);
  //     this.toggleClass(element, 'selected')
  //   } else {
  //     this.selectedPlayer = undefined
  //     this.toggleClass(element, 'active-table-row')
  //   }
  // }

  onBtnClick(player: Player, action: string) {
    switch (action) {
      case 'add': {
        this.midiService.addPlayer().subscribe(async (data) => {
          this.playerAddedEvent.emit(data)
        });
        break
      }
      case 'remove': {
        this.playerRemovedEvent.emit(this.player)
        break
      }
    }
  }

  initBtnClicked() {
    this.onBtnClick(this.player, 'add')
  }

  onInstrumentSelected(instrument: Instrument, player: Player) {
    player.instrumentId = instrument.id
    this.midiService.updatePlayer(player.id, PlayerUpdateType.INSTRUMENT, instrument.id).subscribe()
  }

  onNoteChange(player: Player, event: { target: any; }) {
    this.midiService.updatePlayer(player.id, PlayerUpdateType.NOTE, event.target.value).subscribe()
  }

  onPass(player: Player, $event: MouseEvent) {
    if (player != undefined)
      this.playerSelectEvent.emit(player);
  }


  onRowClick(player: Player, event: MouseEvent) {
    // let element = document.getElementById("player-row-" + player.id)
    // if (this.selectedPlayer == undefined) {
    //   this.selectedPlayer = player
    //   this.playerSelectEvent.emit(player);
    //   this.toggleClass(element, 'selected')
    // } else {
    //   this.selectedPlayer = undefined
    //   this.toggleClass(element, 'active-table-row')
    // }
  }
}
