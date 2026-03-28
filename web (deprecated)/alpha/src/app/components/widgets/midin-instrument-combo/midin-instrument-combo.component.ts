import {
  AfterContentChecked,
  AfterContentInit,
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { Instrument } from 'src/app/models/instrument';
import { MessageListener } from 'src/app/models/message-listener';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-midin-instrument-combo',
  templateUrl: './midin-instrument-combo.component.html',
  styleUrls: ['./midin-instrument-combo.component.css'],
})
export class MidinInstrumentComboComponent
  implements
  MessageListener,
  OnDestroy,
  OnInit,
  AfterViewInit,
  AfterContentInit,
  AfterContentChecked {
  @Output()
  instrumentSelectEvent = new EventEmitter<Instrument>();

  @Input()
  instruments!: Instrument[];

  @Output()
  selectionIndex!: number;

  @Output()
  selectedInstrumentId!: number;

  @Input()
  identifier!: string;

  constructor(private uiService: UiService) {

  }


  ngAfterContentInit(): void { }

  ngAfterViewInit(): void { }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }

  ngOnInit(): void {
    this.uiService.addListener(this);
  }

  ngAfterContentChecked(): void {
    if (
      this.selectionIndex == undefined &&
      this.instruments != undefined &&
      this.instruments.length > 0
    )
      this.selectionIndex = 0;
  }

  notify(messageType: number, _message: string, messageValue: number) {
    // console.log("NOTIFIED")
    if (messageType == Constants.INSTRUMENT_SELECTED) {
      let instrument = this.instruments.filter(
        (instrument) => instrument.id == messageValue
      );
      if (instrument.length > 0) {
        this.selectionIndex = this.instruments.indexOf(instrument[0]);
        this.selectedInstrumentId = instrument[0].id;
      }
    }
  }

  onSelectionChange() {
    this.instrumentSelectEvent.emit(this.instruments[this.selectionIndex]);
    this.uiService.notifyAll(
      Constants.STATUS,
      this.instruments[this.selectionIndex].name + ' selected.',
      0
    );
  }

  setIndexForInstrument() {
    this.instruments
      .filter((i) => i.id == this.selectedInstrumentId)
      .forEach((ins) => {
        this.selectionIndex = this.instruments.indexOf(ins);
      });
  }
}
