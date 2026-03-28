import { Component } from '@angular/core';
import { Constants } from 'src/app/models/constants';
import { UiService } from 'src/app/services/ui.service';

@Component({
  selector: 'app-set-nav',
  templateUrl: './set-nav.component.html',
  styleUrls: ['./set-nav.component.css']
})
export class SetNavComponent {
  constructor(private uiService: UiService) {
    // uiService.addListener(this)
  }


  onClick(action: string) {
    this.uiService.notifyAll(Constants.COMMAND, action, 0)
  }
}
