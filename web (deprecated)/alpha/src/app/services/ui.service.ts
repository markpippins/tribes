import { Injectable } from '@angular/core';
import { MessageListener } from '../models/message-listener';

@Injectable({
  providedIn: 'root',
})

export class UiService {
  listeners: MessageListener[] = [];

  constructor() { }

  addListener(listener: MessageListener) {
    // Add check to prevent duplicate listeners
    if (!this.listeners.includes(listener)) {
      this.listeners.push(listener);
    }
    else console.log("addListener again")
  }

  removeListener(listener: MessageListener) {
    this.listeners = this.listeners.filter(l => l !== listener);
  }

  notifyAll(_messageType: number, _message: string, _messageValue: any) {
    this.listeners.forEach((l) =>
      l.notify(_messageType, _message, _messageValue)
    );
  }

  setSelectValue(id: string, val: any) {
    // @ts-ignore
    let element = document.getElementById(id);
    if (element != null) {
      // @ts-ignore
      element.selectedIndex = val;
    }
  }

  toggleClass(el: any, className: string) {
    if (el.className.indexOf(className) >= 0) {
      el.className = el.className.replace(className, '');
    } else {
      el.className += className;
    }
  }

  hasClass(el: any, classNameA: string) {
    return el.className.indexOf(classNameA) >= 0;
  }

  swapClass(el: any, classNameA: string, classNameB: string) {
    if (el.className.indexOf(classNameA) >= 0) {
      el.className = el.className.replace(classNameA, classNameB);
    } else if (el.className.indexOf(classNameB) >= 0) {
      el.className = el.className.replace(classNameB, classNameA);
    }
  }

  replaceClass(el: any, classNameA: string, classNameB: string) {
    if (el.className.indexOf(classNameA) >= 0) {
      el.className = el.className.replace(classNameA, classNameB);
    }
  }

  removeClass(el: any, classNameA: string) {
    if (el.className.indexOf(classNameA) >= 0) {
      el.className = el.className.replace(classNameA, '');
    }
  }

  addClass(el: any, classNameA: string) {
    if (el.className.indexOf(classNameA) == -1) {
      el.className = el.className.replace(classNameA, '');
    }
  }

  reverseSortById(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.id < b.id) {
        return 1;
      }
      if (a.id > b.id) {
        return -1;
      }
      return 0;
    });
  }

  sortById(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.id > b.id) {
        return 1;
      }
      if (a.id < b.id) {
        return -1;
      }
      return 0;
    });
  }

  sortByName(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.name.toLowerCase() > b.name.toLowerCase()) {
        return 1;
      }
      if (a.name.toLowerCase() < b.name.toLowerCase()) {
        return -1;
      }
      return 0;
    });
  }

  sortAlphabetically(data: string[]): string[] {
    return data.sort((a, b) => {
      if (a.toLowerCase() > b.toLowerCase()) {
        return 1;
      }
      if (a.toLowerCase() < b.toLowerCase()) {
        return -1;
      }
      return 0;
    });
  }

  sortByPosition(data: any[]): any[] {
    return data.sort((a, b) => {
      if (a.position > b.position) {
        return 1;
      }
      if (a.position < b.position) {
        return -1;
      }
      return 0;
    });
  }

  getNoteForValue(value: number, scale: string[]) {
    let note = value;
    while (note > 11) note = note - 12;

    let octave = 1;
    for (let i = 0; i < value; i += 12) octave++;
    return scale[note] + octave;
  }
}
