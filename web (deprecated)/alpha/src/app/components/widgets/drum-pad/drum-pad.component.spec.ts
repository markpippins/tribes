import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DrumPadComponent } from './drum-pad.component';

describe('DrumPadComponent', () => {
  let component: DrumPadComponent;
  let fixture: ComponentFixture<DrumPadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DrumPadComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DrumPadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
