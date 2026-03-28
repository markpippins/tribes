import { Component, EventEmitter, Input, OnInit, OnDestroy, Output } from '@angular/core';
import { Instrument } from '../../models/instrument';
import { MidiService } from '../../services/midi.service';
import { UiService } from 'src/app/services/ui.service';
import { Panel } from 'src/app/models/panel';
import { ControlCode } from 'src/app/models/control-code';
import { MessageListener } from 'src/app/models/message-listener';

@Component({
  selector: 'app-slider-panel',
  templateUrl: './slider-panel.component.html',
  styleUrls: ['./slider-panel.component.css'],
})
export class SliderPanelComponent implements OnDestroy, OnInit, MessageListener {

  @Input()
  instrumentId!: number;

  @Input()
  channel = 1;

  pnls: Map<string, Panel[]> = new Map();

  @Output()
  channelSelectEvent = new EventEmitter<number>();

  @Input()
  instrument!: Instrument | undefined;

  // @Input()
  instruments!: Instrument[];

  @Output()
  configModeOn = false;

  constructor(private midiService: MidiService, private uiService: UiService) { }

  ngOnDestroy(): void {
    this.uiService.removeListener(this);
  }

  ngOnInit(): void {
    this.uiService.addListener(this);
    this.onSelect(10);
    this.midiService.allInstruments().subscribe(async (data) => {
      this.instruments = this.uiService.sortByName(data);
      this.buildPanelMap(this.instruments);
    });
  }

  notify(_messageType: number, _message: string) {
    // console.log("NOTIFIED")
  }

  onSelect(selectedChannel: number) {
    // this.instrument = undefined;
    this.channel = selectedChannel;
    if (this.instrument?.deviceName) {
      this.midiService
        .instrumentInfoByChannel(this.instrument.deviceName, selectedChannel - 1)
        .subscribe(async (data) => {
          this.instrument = data;
          this.channelSelectEvent.emit(selectedChannel);
        });
    }
  }

  getRangeColor() {
    return 'slategrey';
  }

  getStrokeWidth() {
    return 25;
  }

  getStyleClass() {
    return 'knob';
  }

  getTextColor() {
    return 'black';
  }

  getValueColor() {
    return 'fuchsia';
  }

  getValueTemplate(name: string) {
    return name;
  }

  buildPanelMap(instruments: Instrument[]) {
    instruments.forEach((instrument) => {
      this.pnls.set(
        instrument.name,
        this.createMap(instrument.controlCodes.map((cc) => cc.name))
      );
    });
  }

  getPanelsForInstrument(name: string): string[] {
    let result: string[] = [];

    this.pnls.get(name)?.forEach((pnl) => {
      if (!result.includes(pnl.name)) result.push(pnl.name.replace('(', ''));
    });

    return result.sort();
  }

  getOtherControlCodes(name: string): string[] {
    let result: string[] = [];
    this.pnls.get(name)?.forEach((pnl) => {
      if (pnl.name == 'Other')
        pnl.children.forEach((child) => {
          let childName = child.name.replace('Other ', '');
          if (!result.includes(childName)) result.push(childName);
        });
    });

    return result.sort();
  }

  getControlCodes(instrument: Instrument, search: string): ControlCode[] {
    return instrument.controlCodes.filter((cc) => cc.name.startsWith(search));
  }

  controlIsBinary(_cc: ControlCode) {
    // return cc.upperBound == 1 && cc.lowerBound == 0 && (cc.captions.map(cap => cap.description).includes('Off'));
    return false;
  }

  controlIsShortRange(cc: ControlCode) {
    return cc.upperBound > 0 && cc.upperBound - cc.lowerBound < 16;
  }

  controlIsWideRange(cc: ControlCode) {
    return (
      (cc.lowerBound == undefined && cc.upperBound == undefined) ||
      (cc.lowerBound == 0 && cc.upperBound == 0) ||
      (cc.upperBound > 1 && cc.upperBound - cc.lowerBound > 15)
    );
  }

  configBtnClicked() {
    this.configModeOn = !this.configModeOn;
  }

  createMap(data: string[]): Panel[] {
    const map: Panel[] = [];

    for (const name of data) {
      const parts = name.split(' ');

      let parent = map.find((panel) => panel.name === parts[0]);

      if (!parent) {
        parent = { name: parts[0], children: [] };
        map.push(parent);
      }

      let child = parent.children.find((panel) => panel.name === name);

      if (!child) {
        child = { name, children: [] };
        parent.children.push(child);
      }
    }

    for (const parent of map) {
      if (parent.children.length === 1) {
        const child = parent.children[0];
        parent.name = 'Other';
        child.name = `${parent.name} ${child.name}`;
        parent.children.push(child);
      } else {
        const common = this.findCommonPrefix(
          parent.children.map((panel) => panel.name)
        );
        parent.name = common || parent.name;
      }
    }

    return map;
  }

  transformPanels(panels: Panel[]): Panel[] {
    const transformedPanels: Panel[] = [];

    panels.forEach((panel) => {
      const [parentPanelName, childPanelName] = panel.name.split(/\s+/);

      if (parentPanelName && !isNaN(Number(parentPanelName))) {
        let parentPanel = transformedPanels.find(
          (n) => n.name === parentPanelName
        );

        if (!parentPanel) {
          parentPanel = { name: parentPanelName, children: [] };
          transformedPanels.push(parentPanel);
        }

        parentPanel.children.push({
          name: childPanelName,
          children: panel.children,
        });
      } else {
        transformedPanels.push({
          name: panel.name,
          children: this.transformPanels(panel.children),
        });
      }
    });

    return transformedPanels;
  }

  findCommonPrefix(strings: string[]): string {
    if (strings.length === 0) {
      return '';
    }

    let prefix = strings[0];

    for (const string of strings) {
      let i = 0;

      while (
        i < prefix.length &&
        i < string.length &&
        prefix[i] === string[i]
      ) {
        i++;
      }

      prefix = prefix.slice(0, i);
    }

    return this.truncateStringAtSecondToLastSpace(prefix);
  }

  truncateStringAtSecondToLastSpace(str: string): string {
    const secondToLastChar = str.charAt(str.length - 2);
    if (secondToLastChar === ' ') {
      return str.slice(0, str.length - 3);
    }
    return str;
  }
}
