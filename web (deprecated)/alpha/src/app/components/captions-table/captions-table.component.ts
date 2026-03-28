import { Component, Input } from '@angular/core';
import { ControlCode } from 'src/app/models/control-code';

@Component({
  selector: 'app-captions-table',
  templateUrl: './captions-table.component.html',
  styleUrls: ['./captions-table.component.css'],
})
export class CaptionsTableComponent {
  @Input()
  controlCode!: ControlCode;

  getRowClass(_caption: string) {
    return 'table-row';
  }

  getCaptionCodes() {
    let codes: string[] = [];
    for (
      let i = this.controlCode.lowerBound;
      i < this.controlCode.upperBound;
      i++
    )
      codes.push(i.toString());
    return codes; 
  }
}
