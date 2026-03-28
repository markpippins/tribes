import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ControlPanel808Component } from './control-panel808.component';

describe('ControlPanel808Component', () => {
  let component: ControlPanel808Component;
  let fixture: ComponentFixture<ControlPanel808Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ControlPanel808Component ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ControlPanel808Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
