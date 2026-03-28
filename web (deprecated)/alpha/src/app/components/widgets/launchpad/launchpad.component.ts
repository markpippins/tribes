import { Component } from '@angular/core';

@Component({
  selector: 'app-launchpad',
  templateUrl: './launchpad.component.html',
  styleUrls: ['./launchpad.component.css'],
})
export class LaunchpadComponent {

  clips = [64, 65, 66, 67, 60, 61, 62, 63, 56, 57, 58, 59, 52, 53, 54, 55]
  sequences = [96, 97, 98, 99, 92, 93, 94, 95, 88, 89, 90, 91, 84, 85, 86, 87]
  record = [48, 49, 50, 51, 44, 45, 46, 47, 40, 41, 42, 43, 36, 37, 38, 39]
  clear = [80, 81, 82, 83, 76, 77, 78, 79, 72, 73, 74, 75, 68, 69, 70, 71]

  // constructor(private midiService: MidiService) {}

  onClick(note: number) {
    console.log(note);
    // this.midiService.sendMessage(MidiMessage.NOTE_ON, 11, note, 120);
    // this.midiService.sendMessage(MidiMessage.NOTE_OFF, 11, note, 120);
  }
}
