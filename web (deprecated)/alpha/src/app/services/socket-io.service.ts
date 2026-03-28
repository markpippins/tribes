import { Injectable } from '@angular/core';
import * as socketIo from 'socket.io-client';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SocketIOService {
  private clientSocket: socketIo.Socket;

  constructor() {
    this.clientSocket = socketIo.connect('http://localhost:8080/api/tick');
  }

  listenToServer(endpoint: string): Observable<any> {
    return new Observable((subscribe) => {
      this.clientSocket.on(endpoint, (data) => {
        console.log(data)
        subscribe.next(data)
      });
    });
  }
}
