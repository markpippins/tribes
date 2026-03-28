import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiniSliderPanelComponent } from './mini-slider-panel.component';

describe('MiniSliderPanelComponent', () => {
  let component: MiniSliderPanelComponent;
  let fixture: ComponentFixture<MiniSliderPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MiniSliderPanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MiniSliderPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
