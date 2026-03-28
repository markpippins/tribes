import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PadsPanelComponent } from './pads-panel.component';

describe('PadsPanelComponent', () => {
  let component: PadsPanelComponent;
  let fixture: ComponentFixture<PadsPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PadsPanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PadsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
