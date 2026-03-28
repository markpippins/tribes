import { Component } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-ticker-nav',
  templateUrl: './ticker-nav.component.html',
  styleUrls: ['./ticker-nav.component.css']
})
export class TickerNavComponent {

  commands: string[] = [];

  constructor(private uiService: UiService) {
    // uiService.addListener(this)
  }


  onClick(action: string) {
    console.log(action + ' clicked')
    this.commands = [action]
    this.uiService.notifyAll(Constants.COMMAND, action, 0)
  }

  getButtonClass(element: string): string {
    let result = 'btn-inactive'
    if (this.commands.includes(element))
      result = 'btn-active'
    return result;
  }
}
