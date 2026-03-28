import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core'
import {Instrument} from "../../../models/instrument"
import { Player } from 'src/app/models/player';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';

@Component({
  selector: 'app-instrument-selector',
  templateUrl: './instrument-selector.component.html',
  styleUrls: ['./instrument-selector.component.css']
})
export class InstrumentSelectorComponent implements OnInit {

  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Input()
  instruments!: Instrument[]

  @Output()
  selectionIndex !: number

  @Input()
  player!: Player

  constructor(private uiService: UiService) {}

  ngOnInit(): void {

  }

  ngAfterContentChecked(): void {
    if (this.instruments != undefined && this.player != undefined) {
      this.setIndexForInstrument()
    }
  }

  onSelectionChange() {
    this.instrumentSelectEvent.emit(this.instruments[this.selectionIndex])
    this.uiService.notifyAll(Constants.STATUS, this.instruments[this.selectionIndex].name + ' selected.', 0)
  }

  setIndexForInstrument() {
    this.instruments.filter(i => i.id == this.player.instrumentId).forEach(ins => {
      this.selectionIndex = this.instruments.indexOf(ins);
    })
  }
}
