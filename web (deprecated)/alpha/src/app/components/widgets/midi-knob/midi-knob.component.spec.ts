import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MidiKnobComponent } from './midi-knob.component';

describe('MidiKnobComponent', () => {
  let component: MidiKnobComponent;
  let fixture: ComponentFixture<MidiKnobComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MidiKnobComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MidiKnobComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
