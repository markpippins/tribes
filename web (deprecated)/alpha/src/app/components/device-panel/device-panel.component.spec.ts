import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DevicePanelComponent } from './device-panel.component';

describe('DevicePanelComponent', () => {
  let component: DevicePanelComponent;
  let fixture: ComponentFixture<DevicePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DevicePanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DevicePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
