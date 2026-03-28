import {
  AfterContentChecked,
  AfterContentInit,
  AfterViewChecked,
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { MessageListener } from 'src/app/models/message-listener';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-button-panel',
  templateUrl: './button-panel.component.html',
  styleUrls: ['./button-panel.component.css'],
})
export class ButtonPanelComponent
  implements
  MessageListener,
  AfterContentInit,
  AfterContentChecked,
  AfterViewInit,
  AfterViewChecked,
  OnInit,
  OnDestroy {
  @Output()
  buttonClickedForIndexEvent = new EventEmitter<number>();

  @Output()
  buttonClickedForCommandEvent = new EventEmitter<string>();

  @Input()
  exclusive = false;

  @Input()
  maxPressed = 0;

  lastPressed!: String;

  @Input()
  identifier = 'symbol';

  @Input()
  messageType = -1;

  @Input()
  colCount = 16;

  @Input()
  symbols!: string[];

  @Input()
  customControls: string[] = ['-', '+', 'C'];
  visibleCustomControls: string[] = [];

  @Input()
  customControlMinCount: number = 0;

  @Input()
  indicators!: boolean[];

  position = this.colCount;
  range: string[] = [];
  overage: string[] = [];
  selections: boolean[] = [];

  symbolCount = 0;

  @Input()
  symbolBtnClassName = 'mini-btn';

  getSymbolBtnSelectedClassName(): string {
    return this.symbolBtnClassName + '-selected';
  }

  @Input()
  controlBtnClassName = 'mini-btn, mini-control-btn';

  @Input()
  overageBtnClassName = 'overage';

  @Input()
  containerClass = 'flex-container-horizontal';

  constructor(private uiService: UiService) {
  }
  ngOnInit(): void {
    this.uiService.addListener(this);
    this.updateDisplay();
  }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }

  ngAfterContentInit(): void {
    // console.log('ngAfterContentInit');
  }

  ngAfterViewInit(): void {
    // console.log('ngAfterViewInit');
  }

  ngAfterContentChecked(): void {
    // console.log('ngAfterContentChecked');
    if (this.symbolCount != this.symbols.length) {
      this.updateDisplay();
      this.symbolCount = this.symbols.length;
    }
    // this.updateSelections();
  }

  ngAfterViewChecked(): void {
    // console.log('ngAfterViewChecked');
    this.updateSelections();
  }

  notify(_messageType: number, _message: string, messageValue: number) {
    // console.log("NOTIFIED")
    if (_messageType == Constants.CLICK && _message == this.identifier)
      this.selections[messageValue] = !this.selections[messageValue];

    if (_messageType == Constants.NUDGE_RIGHT && _message == this.identifier)
      this.nudgeRight(this.symbols);

    this.updateSelections();
  }

  updateDisplay() {
    this.position = this.colCount;
    this.selections = [];
    this.range = [];

    this.symbols.forEach((s) => {
      this.selections.push(false);
      if (this.range.length < this.colCount) this.range.push(s);
    });

    this.visibleCustomControls = [];
    let index = 0;
    while (
      this.range.length + this.overage.length < this.colCount &&
      index < this.customControls.length
    )
      this.visibleCustomControls.push(this.customControls[index++]!);

    this.overage = [];

    while (
      this.range.length +
      this.visibleCustomControls.length +
      this.overage.length <
      this.colCount
    )
      this.overage.push('');

    // this.overage.forEach(() => this.indicators.push(false))
  }

  onForwardClicked() {
    if (this.range[this.colCount - 1] == this.symbols[this.symbols.length - 1])
      return;

    if (this.position >= this.symbols.length) return;

    this.range = [];
    this.overage = [];

    // this.ticksPosition += this.colCount
    while (this.range.length < this.colCount + this.customControlMinCount) {
      if (this.position == this.symbols.length) break;
      else this.range.push(this.symbols[this.position++]!);
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');

    this.updateSelections();
  }

  onBackClicked() {
    if (this.position == this.colCount) return;

    this.range = [];
    this.overage = [];
    this.position -= this.colCount * 2;
    if (this.position < 0) this.position = 0;

    while (
      this.position < this.symbols.length &&
      this.overage.length + this.range.length < this.colCount
    ) {
      while (this.position == this.symbols.length) this.overage.push('');
      this.range.push(this.symbols[this.position++]!);
    }

    while (this.range.length + this.overage.length < this.colCount)
      this.overage.push('');
    this.updateSelections();
  }

  onSymbolClick(index: number, event: Event) {
    this.selections[index] = !this.selections[index];
    this.uiService.swapClass(
      event.target,
      this.getSymbolBtnSelectedClassName(),
      this.symbolBtnClassName
    );
    this.buttonClickedForIndexEvent.emit(index);
  }

  onCommandClick(index: number, event: Event) {
    this.selections[index] = !this.selections[index];
    this.uiService.swapClass(
      event.target,
      this.getSymbolBtnSelectedClassName(),
      this.symbolBtnClassName
    );
    this.buttonClickedForCommandEvent.emit(this.visibleCustomControls[index]);
  }

  getButtonId(pos: number, over: boolean): string {
    if (over)
      this.identifier +
        '-' +
        this.overageBtnClassName +
        '-' +
        String(this.position + pos);
    return (
      this.identifier +
      '-' +
      this.symbolBtnClassName +
      '-' +
      String(this.position + pos)
    );
  }

  updateSelections() {
    let index = this.position - this.colCount;
    this.selections.forEach((_t) => {
      let id = this.getButtonId(index, false);
      let element = document.getElementById(id);
      if (element != undefined)
        if (this.selections[index]) {
          if (
            !this.uiService.hasClass(
              element,
              this.getSymbolBtnSelectedClassName()
            )
          )
            this.uiService.swapClass(
              element,
              this.symbolBtnClassName,
              this.getSymbolBtnSelectedClassName()
            );
        } else {
          if (
            this.uiService.hasClass(
              element,
              this.getSymbolBtnSelectedClassName()
            )
          )
            this.uiService.replaceClass(
              element,
              this.getSymbolBtnSelectedClassName(),
              this.symbolBtnClassName
            );
        }
      index++;
    });
  }

  toggleAll() {
    let num = 0;
    this.selections.forEach((_t) => {
      this.selections[num] = !this.selections[num];
    });
    this.updateDisplay();
  }

  toggleInterval(interval: number) {
    let num = 0;
    this.selections.forEach((_t) => {
      if (num % interval == 0) this.selections[num] = !this.selections[num];
      num++;
    });

    this.updateDisplay();
  }

  nudgeRight(data: any[]) {
    let temp: any[] = [];
    let length = data.length;
    data.forEach((_e) => temp.push(_e));
    data = [];
    data.push(temp[temp.length - 1]);
    temp.forEach((_t) => {
      if (data.length < length) {
        data.push(_t);
      }
    });

    this.updateDisplay();
  }
}
