import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransportControlComponent } from './transport-control.component';

describe('TransportControlComponent', () => {
  let component: TransportControlComponent;
  let fixture: ComponentFixture<TransportControlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TransportControlComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransportControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
