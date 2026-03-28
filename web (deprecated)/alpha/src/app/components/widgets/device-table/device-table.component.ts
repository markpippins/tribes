import { Component, Input } from '@angular/core';
import { Device } from 'src/app/models/device';

@Component({
  selector: 'app-device-table',
  templateUrl: './device-table.component.html',
  styleUrls: ['./device-table.component.css'],
})
export class DeviceTableComponent {
  @Input()
  devices!: Device[];

  removeList = ['com.sun.media.sound.'];

  shortenedName(inputString: string): string {
    if (inputString != undefined)
      this.removeList.forEach((word) => {
        if (inputString.includes(word))
          inputString = inputString.replace(word, '');
      });
    return inputString;
  }

  getRowClass(device: Device): string {
    if (device.description.includes('External MIDI Port')) return 'pad-1-dark';

    if (device.description.includes('Internal software synthesizer')) return 'pad-2-dark';

    if (device.description.includes('Software MIDI Synthesizer')) return 'pad-4-light';

    if (device.description.includes('Software sequencer')) return 'pad-7';

    if (device.description.includes('MAPPER')) return 'pad-3-dark';

    if (device.description.includes('No details available')) return 'pad-8-light';

    return 'table-row';
  }
}
