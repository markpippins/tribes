import { Component, Input } from '@angular/core';
import { Player } from 'src/app/models/player';
import { Ticker } from 'src/app/models/ticker';

@Component({
  selector: 'app-status-readout',
  templateUrl: './status-readout.component.html',
  styleUrls: ['./status-readout.component.css']
})
export class StatusReadoutComponent {

    @Input()
    ticker!: Ticker;

    @Input()
    players!: Player[]

    @Input()
    consoleOutput!: string[]
  }
