import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatusReadoutComponent } from './status-readout.component';

describe('StatusReadoutComponent', () => {
  let component: StatusReadoutComponent;
  let fixture: ComponentFixture<StatusReadoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StatusReadoutComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StatusReadoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
