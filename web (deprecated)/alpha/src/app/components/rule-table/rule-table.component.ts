import { AfterContentChecked, OnInit, OnDestroy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MidiService } from '../../services/midi.service';
import { Player } from '../../models/player';
import { Rule } from '../../models/rule';
import { UiService } from 'src/app/services/ui.service';
import { Constants } from 'src/app/models/constants';
import { RuleUpdateType } from 'src/app/models/rule-update-type';
import { MessageListener } from 'src/app/models/message-listener';

@Component({
  selector: 'app-rule-table',
  templateUrl: './rule-table.component.html',
  styleUrls: ['./rule-table.component.css'],
})
export class RuleTableComponent implements MessageListener, AfterContentChecked, OnInit, OnDestroy {

  @Output()
  ruleChangeEvent = new EventEmitter<Player>();

  EQUALS = 0;
  GREATER_THAN = 1;
  LESS_THAN = 2;
  MODULO = 3;
  COMPARISON = ['=', '>', '<', '%', '*', '/'];
  TICK = 0;
  BEAT = 1;
  BAR = 2;
  PART = 3;
  POSITION = 4;
  OPERATOR = [
    'Tick',
    'Beat',
    'Bar',
    'Part',
    'Whole Beat',
    'Ticks',
    'Beats',
    'Bars',
    'Parts',
  ];

  interval = 0.25;

  @Output()
  ruleSelectEvent = new EventEmitter<Rule>();

  @Input()
  player!: Player;
  ruleCols: string[] = [
    // 'ID',
    'Operator',
    'Op',
    'Value',
    'Part',
  ];

  intervalSet = false;
  selectedRule!: Rule;

  constructor(private midiService: MidiService, private uiService: UiService) {
  }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }

  ngOnInit(): void {
    this.uiService.removeListener(this);
  }

  notify(_messageType: number, _message: string, _messageValue: number) {
    // console.log("NOTIFIED")
    if (_messageType == Constants.COMMAND) {
      console.log("COMMAND")
      switch (_message) {
        case 'rule-add': {
          console.log("rule table - rule-add")
          this.midiService.addRule(this.player).subscribe(async (data) => {
            this.player.rules.push(data);
            this.ruleChangeEvent.emit(this.player);
          });
          break;
        }
      }
    }
  }

  ngAfterContentChecked(): void {

    this.getRules().forEach((rule) => {
      let op = 'operatorSelect-' + rule.id;
      this.uiService.setSelectValue(op, rule.operator);
    });
    this.getRules().forEach((rule) => {
      let co = 'comparisonSelect-' + rule.id;
      this.uiService.setSelectValue(co, rule.comparison);
    });

    // if (!this.intervalSet)
    //   this.updateDisplay();
  }

  // updateDisplay(): void {
  //   this.midiService.tickerStatus().subscribe(data => {
  //       this.interval = 1 / data.ticksPerBeat
  //       this.intervalSet = true;
  //     });
  // }

  onRowClick(rule: Rule) {
    this.selectedRule = rule
    this.ruleSelectEvent.emit(rule);
  }

  getRules(): Rule[] {
    return this.player == undefined ? [] : this.player.rules;
  }

  onOperatorChange(rule: Rule, event: { target: any }) {
    let value = this.OPERATOR.indexOf(event.target.value);
    this.midiService
      .updateRule(rule.id, RuleUpdateType.OPERATOR, value)
      .subscribe();
    rule.operator = value;
    // let op = 'operatorSelect-' + rule.id
    this.uiService.setSelectValue(event.target, value);
    this.uiService.notifyAll(
      Constants.STATUS,
      this.OPERATOR[value] + ' selected.',
      0
    );
  }

  onComparisonChange(rule: Rule, event: { target: any }) {
    let value = this.COMPARISON.indexOf(event.target.value);
    this.midiService
      .updateRule(rule.id, RuleUpdateType.COMPARISON, value)
      .subscribe();
    rule.comparison = value;
    this.uiService.setSelectValue(event.target, value);
    this.uiService.notifyAll(
      Constants.STATUS,
      this.COMPARISON[value] + ' selected.',
      0
    );
  }

  onValueChange(rule: Rule, event: { target: any }) {
    this.midiService
      .updateRule(rule.id, RuleUpdateType.VALUE, event.target.value)
      .subscribe();
  }

  onPartChange(rule: Rule, event: { target: any }) {
    this.midiService
      .updateRule(rule.id, RuleUpdateType.PART, event.target.value)
      .subscribe();
  }

  btnClicked(rule: Rule, command: string) {

    if (this.player.id > 0)
      switch (command) {
        case 'add': {
          this.midiService.addRule(this.player).subscribe(async (data) => {
            this.player.rules.push(data);
            this.ruleChangeEvent.emit(this.player);
          });
          break;
        }

        case 'remove': {
          this.player.rules = this.player.rules.filter((r) => r.id != rule.id);
          this.midiService.removeRule(this.player, rule).subscribe();
          this.ruleChangeEvent.emit(this.player);
          break;
        }
      }

    this.uiService.notifyAll(Constants.COMMAND, command, 0)

  }

  initBtnClick() {
    if (this.player.id > 0)
      this.midiService.addRule(this.player).subscribe(async (data) => {
        this.player.rules.push(data);
      });
  }

  onPass(rule: Rule) {
    if (rule != undefined && this.selectedRule != undefined)
      this.ruleSelectEvent.emit(rule);
  }

  getMuteButtonClass(_rule: Rule): string {
    return '';
  }

  getRowClass(rule: Rule): string {
    return 'table-row' + (rule === this.selectedRule ? ' selected' : '');
  }

  onClick(action: string) {
    console.log(action + " clicked")
    this.btnClicked(this.selectedRule, action);
  }
}
