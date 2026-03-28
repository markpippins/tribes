import {Component, OnInit} from '@angular/core'
import { Instrument } from 'src/app/models/instrument'
import { UiService } from 'src/app/services/ui.service'
import {Device} from "../../models/device"
import {MidiService} from "../../services/midi.service"
import { ControlCode } from 'src/app/models/control-code'

@Component({
  selector: 'app-device-panel',
  templateUrl: './device-panel.component.html',
  styleUrls: ['./device-panel.component.css']
})
export class DevicePanelComponent implements  OnInit {

  constructor(private midiService: MidiService, private uiService: UiService) {
  }
  instruments!: Instrument[]
  devices!: Device[]
  ports: Device[] = []
  synths: Device[] = []
  other: Device[] = []
  unknown: Device[] = []
  selectedInstrument!: Instrument
  selectedControlCode!: ControlCode

  ngOnInit(): void {
    this.midiService.allDevices().subscribe(data => {
      this.devices = data
      this.ports = this.devices.filter(d => d.description.toLowerCase().indexOf('port') > -1)
      this.synths = this.devices.filter(d => d.description.toLowerCase().indexOf('synth') > -1)
      this.unknown = this.devices.filter(d => d.description.toLowerCase().indexOf('no details') > -1)
      this.other = this.devices.filter(d => !this.ports.includes(d) && ! this.synths.includes(d) && !this.unknown.includes(d))
    })

    this.midiService.allInstruments().subscribe(data => {
      this.instruments = this.uiService.sortByName(data)
    })
  }

  onInstrumentSelected(instrument: Instrument) {
    this.selectedInstrument = instrument
  }

  onControlCodeSelected(controlCode: ControlCode) {
    this.selectedControlCode = controlCode
  }

  onRowClick(instrument: Instrument) {
      this.selectedInstrument = instrument
  }

  getDeviceNames() {
    return this.devices.map(d => d.name)
  }
}
