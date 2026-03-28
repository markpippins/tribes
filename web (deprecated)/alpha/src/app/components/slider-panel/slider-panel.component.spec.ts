import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SliderPanelComponent } from './slider-panel.component';

describe('SliderPanelComponent', () => {
  let component: SliderPanelComponent;
  let fixture: ComponentFixture<SliderPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SliderPanelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SliderPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
